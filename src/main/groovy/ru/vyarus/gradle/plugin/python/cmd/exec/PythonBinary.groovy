package ru.vyarus.gradle.plugin.python.cmd.exec

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.logging.LogLevel
import ru.vyarus.gradle.plugin.python.cmd.LoggedCommandCleaner
import ru.vyarus.gradle.plugin.python.cmd.docker.ContainerManager
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerConfig
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerFactory
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.DurationFormatter
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

import java.nio.file.Paths
import java.util.function.Supplier

/**
 * Abstraction for python binary paths logic. Separated from {@link ru.vyarus.gradle.plugin.python.cmd.Python} to
 * encapsulate system-specific python binary search and python command formatting logic.
 * <p>
 * Also encapsulates docker-specific logic.
 *
 * @author Vyacheslav Rusakov
 * @since 30.08.2022
 */
@SuppressWarnings(['ConfusingMethodName', 'MethodCount'])
@CompileStatic
final class PythonBinary {

    private static final String PYTHON3 = 'python3'

    private static final String SPACE = ' '
    private static final String NL = '\n'

    private final Environment environment

    // pre-init

    private final String sourcePythonPath
    private final String sourcePythonBinary

    private boolean validateBinary = true
    private ContainerManager dockerManager
    private DockerConfig dockerConfig

    // keeping actual work dir and environment here is important for docker to prevent redundant container
    // restarts due to false-detection of config changes
    private String workDir
    private final Map<String, Object> envVars = [:]

    // post init

    private boolean initialized
    private String executable
    // run through cmd on win (when direct executable called)
    private boolean withCmd
    // set when calling custom binary by path instead of global (required to rewrite path to absolute)
    private boolean customBinaryPath

    // special cleaners for logged commands to hide sensitive data (like passwords)
    private final List<LoggedCommandCleaner> logCleaners = []

    PythonBinary(Environment environment, String pythonPath, String binary) {
        this.environment = environment
        this.sourcePythonPath = pythonPath
        this.sourcePythonBinary = binary

        // actual initialization is delayed to simplify configuration (avoid putting everything into constructor)
        // see #init method
    }

    void validateSystemBinary(boolean validateBinary) {
        beforeInit('System binary check must be set before initialization')
        this.validateBinary = validateBinary
    }

    void withDocker(DockerConfig docker) {
        beforeInit('Docker config must be set before initialization')
        if (docker) {
            this.dockerConfig = docker
            this.dockerManager = DockerFactory.getContainer(docker, environment)
        }
    }

    void addLogCleaner(LoggedCommandCleaner cleaner) {
        logCleaners.add(cleaner)
    }

    void workDir(String workDir) {
        this.workDir = workDir
    }

    String getWorkDir() {
        return workDir
    }

    Map<String, Object> getEnvVars() {
        return envVars
    }

    String getExecutable() {
        init()
        executable
    }

    /**
     * Updates separators in path according to target environment. Important for docker when windows host could
     * execute linux container.
     *
     * @param path path
     * @return path with corrected separators or path as is if docker not used
     */
    String targetOsCanonicalPath(String path) {
        return docker ? dockerManager.canonicalPath(path) : path
    }

    /**
     * @param sysExecutableProvider sys.executable value provider
     * @param sysPrefixProvider sys.prefix value provider
     * @return directory under python home containing python binary (always absolute path)
     * @see {@link ru.vyarus.gradle.plugin.python.cmd.Python#getBinaryDir()}
     */
    String getBinaryDir(Supplier<String> sysExecutableProvider, Supplier<String> sysPrefixProvider) {
        init()
        // use resolved executable to avoid incorrect resolution in case of venv inside venv
        String path = customBinaryPath ? environment.file(executable).absolutePath : sysExecutableProvider.get()
        int idx = path.lastIndexOf(File.separator)

        String res
        if (path.empty || idx <= 0) {
            // just guess by home dir (yes, I know, this MIGHT be incorrect in some cases, but should be ok
            // for virtualenvs used in majority of cases)
            path = sysPrefixProvider.get()
            res = CliUtils.pythonBinPath(path, windows)
        } else {
            // cut off binary
            res = path[0..idx - 1]
        }
        if (docker) {
            // in case of docker path might be either local or inside docker (depending on resolution method)
            // this way it would always be a docker path
            res = dockerManager.toDockerPath(res)
        }
        return res
    }

    boolean isWindows() {
        return docker ? dockerManager.windows : Os.isFamily(Os.FAMILY_WINDOWS)
    }

    boolean isDocker() {
        return dockerManager != null
    }

    /**
     * @return string to identify python (within current project)
     */
    String getIdentity() {
        return buildIdentity(sourcePythonPath, sourcePythonBinary, docker)
    }

    // checks (absolute!) path for existence
    // method required for correct checking for files existence inside docker
    // LIMITED TO FILES
    boolean exists(String path) {
        init()
        if (docker) {
            // if path leads inside project then it might be checked locally
            String localPath = dockerManager.toLocalPath(path)
            if (localPath != null) {
                return new File(localPath).exists()
            }
            // testing path inside docker
            String res = rawExec([windows ? "IF EXIST \"$path\" ECHO exists"
                                          : "test -f $path && echo \"exists\""] as String[])
            return res == 'exists'
        }

        File file = new File(path)
        boolean res = file.exists()
        environment.logger.info('File {} exists: {}', file.absolutePath, res)
        return res
    }

    void exec(Object args,
              List<String> pythonArgs,
              List<String> extraArgs,
              OutputStream outputConsumer,
              LogLevel logLevel) {
        init()
        String exec = getCommandBinary(workDir)
        String[] cmdArgs = getCommandArguments(workDir, args, pythonArgs, extraArgs)

        // important to show arguments as array to easily spot args parsing problems
        environment.logger.info('Python arguments: {}', cmdArgs)

        String[] cmd = [exec]
        cmd += cmdArgs
        if (docker) {
            // change paths (according to docker mapping)
            // performed here in order to see actual command in logs and error message, plus, cleanups applied here
            dockerManager.convertCommand(cmd)
            // called here to show container start/stop logs before executed command
            prepareEnvironment()
        }

        String formattedCommand = cleanLoggedCommand(cmd)
        environment.logger.log(logLevel, "[python] $formattedCommand")

        long start = System.currentTimeMillis()
        int res = rawExec(cmd, outputConsumer)
        environment.logger.info('Python execution time: {}',
                DurationFormatter.format(System.currentTimeMillis() - start))
        if (res != 0) {
            // duplicating output in error message to be sure it would be visible
            String out = reformatOutputForException(outputConsumer)
            throw new PythonExecutionFailed("Python call failed: $formattedCommand"
                    + (out ? '\n\n\tOUTPUT:\n' + out : ''))
        }
    }

    /**
     * Delayed initialization is required to reduce constructor arguments and simplify usage.
     * It is extremely important to have all properties configured before first python execution in context of docker
     * (e.g. if env. vars would not be available on first run - it would lead to redundant container restart
     * (if any vars used))
     */
    @SuppressWarnings('SynchronizedOnThis')
    void init() {
        if (initialized) {
            return
        }
        synchronized (this) {
            if (!initialized) {
                this.executable = findPython(environment, sourcePythonPath, sourcePythonBinary, validateBinary)
                // direct executable must be called with cmd
                // (https://docs.gradle.org/4.1/dsl/org.gradle.api.tasks.Exec.html)
                this.withCmd = sourcePythonPath && windows
                // custom python path used (which may be relative and conflict with workDir)
                this.customBinaryPath = sourcePythonPath as boolean
                this.initialized = true
            }
        }
    }

    /**
     * Shortcut to cache values related to the same python (not the same instance, but the same python, related
     * to current gradle project).
     * <p>
     * Actual cache key also includes python path to avoid clashes when multiple pythons used. If python path
     * contain current project path (subproject in multi-module environment) then project cache would be used,
     * otherwise global cache.
     *
     * @param key cache key
     * @param value value computation action
     * @return cached or computed value
     */
    public <T> T getOrCompute(String key, Supplier<T> value) {
        String id = identity
        if (id.startsWith(environment.relativeRootPath(environment.projectDir.absolutePath))) {
            return environment.projectCache("$key:$id", value)
        }
        return environment.globalCache("$key:$id", value)
    }

    private String buildIdentity(String pythonPath, String binary, boolean docker) {
        return ((pythonPath == null ? '' : CliUtils.canonicalPath(environment.file(pythonPath))
                .replace(environment.rootDir.path + '/', '')) + (binary ?: '')) +
                (docker ? '[docker]' : '')
    }

    // @Memoized can't be used due to configuration cache
    private String getPythonBinary(String pythonPath, String binary, boolean python3Available) {
        // not identity for back reference (..) sub projects because it would mix with root project and
        // produce incorrect path for one or another. From the other side, path like ../somthing could be common
        // for multiple sub projects and so it makes sense to cache it globally
        String id = pythonPath?.startsWith('..') ? "$pythonPath:$binary" : buildIdentity(pythonPath, binary, docker)
        return environment.globalCache("python.binary:$id") {
            String res = binary ?: 'python'
            if (pythonPath) {
                String path = pythonPath + (pythonPath.endsWith(File.separator) ? '' : File.separator)
                // $pythonPath/$binaryName(.exe)
                res = windows ? "${path}${res}.exe" : "$path$res"
            } else if (!binary && python3Available) {
                res = PYTHON3
            }
            return res
        } as String
    }

    // @Memoized can't be used due to configuration cache
    private File findSystemBinary(String binary) {
        return environment.globalCache("system.binary:$binary") {
            // manually searching in system path to point to possibly incorrect PATH variable used in process
            // (might happen when process not started from user shell)
            return CliUtils.searchSystemBinary(binary)
        } as File
    }

    @SuppressWarnings('Instanceof')
    private String[] prepareArgs(Object args, List<String> pythonArgs, List<String> extraArgs) {
        String[] cmd = CliUtils.parseArgs(args)
        detectAndWrapCommand(cmd)
        if (pythonArgs) {
            cmd = CliUtils.mergeArgs(pythonArgs, cmd)
        }
        if (extraArgs) {
            cmd = CliUtils.mergeArgs(cmd, extraArgs)
        }
        return cmd
    }

    /**
     * Detect python command call (-c) and wrap command argument if required (on linux).
     *
     * @param cmd command to execute
     */
    private void detectAndWrapCommand(String[] cmd) {
        boolean moduleCall = false
        cmd.eachWithIndex { String arg, int i ->
            if (arg == '-m') {
                moduleCall = true
            }
            if (!moduleCall && arg == '-c' && i + 2 == cmd.length) {
                // wrap command to grant cross-platform compatibility (simple -c "string" is not always executed)
                cmd[i + 1] = CliUtils.wrapCommand(cmd[i + 1], windows)
            }
        }
    }

    private String findPython(Environment environment, String pythonPath, String binary, boolean validateBinary) {
        String normalizedPath = pythonPath == null ? null : Paths.get(pythonPath).normalize().toString()
        // detecting python3 binary only if default python usage assumed (to avoid redundant python calls)
        boolean python3found = (binary || pythonPath) ? false : detectPython3Binary()
        String executable = getPythonBinary(normalizedPath, binary, python3found)
        // search not performed for docker!
        if (validateBinary && !pythonPath && !docker) {
            // search would fail if no binary found in system paths
            environment.logger.info('Found python binary: {}', findSystemBinary(executable).absolutePath)
        }
        return executable
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('AssignmentToStaticFieldFromInstanceMethod')
    private boolean detectPython3Binary() {
        return environment.globalCache("python3.binary:${docker ? dockerManager.containerName : ''}") {
            // on windows python binary could not be named python3
            return !windows && rawExec([PYTHON3, '--version'] as String[]) != null
        }
    }

    private String cleanLoggedCommand(String[] cmd) {
        String commandLine = cmd.join(SPACE)
        String res = commandLine.replace('\r', '').replace(NL, SPACE)
        logCleaners.each { res = it.clear(res) }
        return res
    }

    private String reformatOutputForException(OutputStream os) {
        os.flush()
        String out = os
        return out ? out.split(NL).collect { '\t\t' + it }.join(NL) + NL : ''
    }

    private void prepareEnvironment() {
        if (dockerConfig.exclusive) {
            environment.logger.lifecycle("[docker] executing command in exclusive container '{}' \n{}",
                    dockerConfig.image, dockerManager.formatContainerInfo(dockerConfig, workDir, envVars))
        } else {
            // first executed command will start container and all subsequent calls would use already
            // started container
            dockerManager.restartIfRequired(dockerConfig, workDir, envVars)
        }
    }

    // command output or null in case of error
    private String rawExec(String[] command) {
        new ByteArrayOutputStream().withStream { os ->
            return rawExec(command, os) == 0 ? os.toString() : null
        }
    }

    // IMPORTANT: single point of execution for all python commands
    @SuppressWarnings('UnnecessarySetter')
    private int rawExec(String[] command, OutputStream out) {
        long start = System.currentTimeMillis()
        int res = -1
        String cmdForLog = cleanLoggedCommand(command)
        try {
            environment.debug(cmdForLog + '    (WorkDir: ' + (workDir != null ? workDir :
                    environment.relativeRootPath(environment.projectDir.path)) + ')')
            if (docker) {
                // required here for direct raw execution case (normally, start logs must appear before
                // executed command)
                if (!dockerManager.started) {
                    dockerManager.restartIfRequired(dockerConfig, workDir, envVars)
                }
                res = dockerConfig.exclusive
                        ? dockerManager.execExclusive(command, out, dockerConfig, workDir, envVars)
                        : dockerManager.exec(command, out)
            } else {
                res = environment.exec(command, out, out, workDir, envVars)
            }
        } finally {
            environment.stat(docker ? dockerManager.containerName : null, cmdForLog, workDir,
                    !customBinaryPath, start, res == 0)
        }
        return res
    }

    /**
     * @param workDir work directory
     * @return execution binary (wrapped, if required for correct execution)
     */
    private String getCommandBinary(String workDir) {
        init()
        boolean wrkDirUsed = workDir as boolean
        // on win non global python could be called only through cmd
        return withCmd ? 'cmd'
                // use absolute python path if work dir set (relative will simply not work)
                : (wrkDirUsed && customBinaryPath ? CliUtils.canonicalPath(environment.file(executable)) : executable)
    }

    /**
     * Additionally, wraps python command call (-c) to properly execute on linux.
     *
     * @param workDir work dir
     * @param args command
     * @return formatted arguments
     */
    private String[] getCommandArguments(String workDir, Object args, List<String> pythonArgs, List<String> extraArgs) {
        init()
        boolean wrkDirUsed = workDir as boolean
        return withCmd ? CliUtils
                .wincmdArgs(executable, environment.projectDir, prepareArgs(args, pythonArgs, extraArgs), wrkDirUsed)
                : prepareArgs(args, pythonArgs, extraArgs)
    }

    private void beforeInit(String message) {
        if (initialized) {
            throw new IllegalStateException(message)
        }
    }
}
