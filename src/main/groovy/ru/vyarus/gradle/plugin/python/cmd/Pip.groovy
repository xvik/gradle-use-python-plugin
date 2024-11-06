package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerConfig
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Pip commands execution utility. Use {@link Python} internally.
 *
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
@CompileStatic
@SuppressWarnings('ConfusingMethodName')
class Pip {

    private static final Pattern VERSION = Pattern.compile('pip ([\\d.]+)')

    public static final String USER = '--user'
    public static final String NO_CACHE = '--no-cache-dir'
    public static final String BREAK_SYSTEM_PACKAGES = '--break-system-packages'
    private static final String INSTALL_TASK = 'install'
    private static final String LIST_TASK = 'list'
    private static final List<String> USER_AWARE_COMMANDS = [INSTALL_TASK, LIST_TASK, 'freeze']
    private static final List<String> INDEX_AWARE_COMMANDS = [INSTALL_TASK, LIST_TASK, 'download', 'wheel']

    private final Python python
    // --user for install, list and freeze tasks
    private boolean userScope = true
    // --no-cache-dir for install task
    // may be changed externally
    boolean useCache = true
    // --break-system-packages for install task
    boolean breakSystemPackages = false
    // --index-url
    String indexUrl
    // --extra-index-url
    List<String> extraIndexUrls = []
    // --trusted-host
    List<String> trustedHosts = []

    Pip(Environment environment) {
        this(environment, null, null)
    }

    Pip(Environment environment, String pythonPath, String binary) {
        this(new Python(environment, pythonPath, binary).logLevel(LogLevel.LIFECYCLE))
    }

    // preferred way for construction because allows configured python instance re-usage
    Pip(Python python) {
        this.python = python

        // do not show passwords when external indexes used with credentials
        python.logCommandCleaner { CliUtils.hidePipCredentials(it) }
    }

    /**
     * Execute install, list and freeze tasks with --user flag (in user scope).
     * Enabled by default!
     *
     * @param inUserScope false to switch to global scope
     * @return pip instance for chained calls
     */
    Pip userScope(boolean inUserScope) {
        this.userScope = inUserScope
        return this
    }

    /**
     * By default pip install will use case, Set to false in order to disable pip installation cache (--no-cache-dir
     * would be applied).
     *
     * @param cache false to disable pip install cache
     * @return pip instance for chained calls
     */
    Pip useCache(boolean cache) {
        this.useCache = cache
        return this
    }

    /**
     * Useful for linux non-default pythons installed with apt (e.g. apt install python3.12). Enabling this option
     * would remove error "This environment is externally managed" and install packages into local user directory
     * (~/.local/lib/python3.12).
     *
     * @param disablePackageSecurity true to disable python's externally managed environment check
     * @return pip instance for chained calls
     */
    Pip breakSystemPackages(boolean disablePackageSecurity) {
        this.breakSystemPackages = disablePackageSecurity
        return this
    }

    /**
     * System binary search is performed only for global python (when pythonPath is not specified). Enabled by default.
     *
     * @param validate true to search python binary in system path and fail if not found
     * @return cli instance for chained calls
     */
    Pip validateSystemBinary(boolean validate) {
        this.python.validateSystemBinary(validate)
        return this
    }

    /**
     * Enable docker support: all python commands would be executed under docker container.
     *
     * @param docker docker configuration (may be null)
     * @return cli instance for chained calls
     */
    Pip withDocker(DockerConfig docker) {
        this.python.withDocker(docker)
        return this
    }

    /**
     * Shortcut for {@link Python#workDir(java.lang.String)}.
     *
     * @param workDir python working directory
     * @return pip instance for chained calls
     */
    Pip workDir(String workDir) {
        python.workDir(workDir)
        return this
    }

    /**
     * Shortcut for {@link Python#environment(java.util.Map)}.
     *
     * @param env environment map
     * @return pip instance for chained calls
     */
    Pip environment(Map<String, Object> env) {
        python.environment(env)
        return this
    }

    /**
     * Set default pip repository (--index-url). Applies only for commands supporting it.
     *
     * @param url url to default pip repository
     * @return pip instance for chained calls
     * @see ru.vyarus.gradle.plugin.python.PythonExtension#indexUrl
     */
    Pip indexUrl(String url) {
        indexUrl = url
        return this
    }

    /**
     * Apply extra pip repositories (--extra-index-url). Applies only for commands supporting it.
     *
     * @param urls urls to pip repositories.
     * @return pip instance for chained calls
     * @see ru.vyarus.gradle.plugin.python.PythonExtension#extraIndexUrls
     */
    Pip extraIndexUrls(List<String> urls) {
        if (urls) {
            this.extraIndexUrls.addAll(urls)
        }
        return this
    }

    /**
     * Apply trusted hosts (--trusted-host). Applied only for {@code pip install}.
     *
     * @param hosts trusted hosts
     * @return pip instance for chained calls
     * @see ru.vyarus.gradle.plugin.python.PythonExtension#trustedHosts
     */
    Pip trustedHosts(List<String> hosts) {
        if (hosts) {
            this.trustedHosts.addAll(hosts)
        }
        return this
    }

    /**
     * Perform pre-initialization and, if required, validate global python binary correctness. Calling this method is
     * NOT REQUIRED: initialization will be performed automatically before first execution. But it might be called
     * in order to throw possible initialization error before some other logic (related to exception handling).
     *
     * @return pip instance for chained calls
     */
    Pip validate() {
        python.validate()
        return this
    }

    /**
     * Install module.
     *
     * @param module module name with version (e.g. 'some==12.3')
     */
    void install(String module) {
        exec("install $module")
    }

    /**
     * Uninstall module.
     *
     * @param module module name
     */
    void uninstall(String module) {
        exec("uninstall $module -y")
        if (isInstalled(module)) {
            // known problem
            throw new GradleException("Failed to uninstall module $module. Try to update pip: " +
                    '\'pip install -U pip\' or try to manually remove package ' +
                    '(probably not enough permissions)')
        }
    }

    /**
     * @param module module to check
     * @return true if module installed
     */
    boolean isInstalled(String module) {
        try {
            // has no output on error, so nothing will appear in log
            readOutput("show $module")
        } catch (PythonExecutionFailed ignored) {
            return false
        }
        return true
    }

    /**
     * Execute command on pip module. E.g. 'install some==12.3'.
     *
     * @param cmd pip command to execute
     */
    void exec(String cmd) {
        python.callModule('pip', applyFlags(cmd))
    }

    /**
     * Calls pip command and return output as string. Preferred way instead of direct python usage to correctly
     * apply --user flag for commands.
     *
     * @param cmd pip command to call
     * @return command execution output
     */
    String readOutput(String cmd) {
        python.withHiddenLog {
            python.readOutput("-m pip ${applyFlags(cmd)}")
        }
    }

    /**
     * May be used to change default configurations.
     *
     * @return python cli instance used to execute commands
     */
    Python getPython() {
        return python
    }

    /**
     * @return pip version string (minor.major.micro)
     */
    String getVersion() {
        python.getOrCompute('pip.version') {
            // first try to parse line to avoid duplicate python call
            Matcher matcher = VERSION.matcher(versionLine)
            if (matcher.find()) {
                // note: this will drop beta postfix (e.g. for 10.0.0b2 version will be 10.0.0)
                return matcher.group(1)
            }
            // if can't recognize version, ask directly
            return python.withHiddenLog {
                python.readOutput('-c \"import pip; print(pip.__version__)\"')
            }
        }
    }

    /**
     * @return pip --version output
     */
    String getVersionLine() {
        return python.getOrCompute('pip.version.line') {
            return python.withHiddenLog {
                python.readOutput('-m pip --version')
            }
        }
    }

    /**
     * Execute pip methods within action in global scope (no matter if user scope configured).
     *
     * @param action  pip action to be executed in global scope
     * @return action result
     */
    public <T> T inGlobalScope(Supplier<T> action) {
        boolean isUserScope = this.userScope
        this.userScope = false
        try {
            return (T) action.get()
        } finally {
            this.userScope = isUserScope
        }
    }

    @Override
    String toString() {
        return versionLine
    }

    private String applyFlags(String cmd) {
        String command = extractCommand(cmd)

        // -- user
        // explicit virtualenv check is required because flag will fail under virtualenv
        if (!cmd.contains(USER) && userScope && USER_AWARE_COMMANDS.contains(command)
                && !python.virtualenv) {
            cmd += " $USER"
        }
        // --no-cache-dir (only for install command)
        if (!useCache && !cmd.contains(NO_CACHE) && cmd.startsWith(INSTALL_TASK)) {
            cmd += " $NO_CACHE"
        }
        // for linux alternative pythons like python3.12 (installed with apt: apt install python3.12)
        // install would complain on direct packages installation with pip
        if (breakSystemPackages && !cmd.contains(BREAK_SYSTEM_PACKAGES) && cmd.startsWith(INSTALL_TASK)) {
            cmd += " $BREAK_SYSTEM_PACKAGES"
        }

        if (indexUrl && INDEX_AWARE_COMMANDS.contains(command)) {
            cmd += " --index-url $indexUrl"
        }

        if (extraIndexUrls && INDEX_AWARE_COMMANDS.contains(command)) {
            extraIndexUrls.each { extraIndexUrl ->
                cmd += " --extra-index-url $extraIndexUrl"
            }
        }
        if (trustedHosts && cmd.startsWith(INSTALL_TASK)) {
            trustedHosts.each { trustedHost ->
                cmd += " --trusted-host $trustedHost"
            }
        }
        cmd
    }

    private String extractCommand(String cmdLine) {
        return cmdLine.split(' ')[0].toLowerCase()
    }
}
