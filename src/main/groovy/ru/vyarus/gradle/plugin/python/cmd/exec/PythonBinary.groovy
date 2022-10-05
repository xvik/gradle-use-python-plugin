package ru.vyarus.gradle.plugin.python.cmd.exec

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import ru.vyarus.gradle.plugin.python.cmd.LoggedCommandCleaner
import ru.vyarus.gradle.plugin.python.cmd.docker.ContainerManager
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerConfig
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerFactory
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.DurationFormatter
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

import java.nio.file.Paths

/**
 * Abstraction for python binary paths logic. Separated from {@link ru.vyarus.gradle.plugin.python.cmd.Python} to
 * encapsulate system-specific python binary search and python command formatting logic.
 * <p>
 * Also encapsulates docker-specific logic.
 *
 * @author Vyacheslav Rusakov
 * @since 30.08.2022
 */
@SuppressWarnings('ConfusingMethodName')
@CompileStatic
final class PythonBinary {

    private static final String PROP_PYTHON3 = 'ru.vyarus.python3.detected'
    private static final String PYTHON3 = 'python3'

    private static final String SPACE = ' '
    private static final String NL = '\n'

    private static final Object SYNC = new Object()

    private final Project project

    // pre-init

    private final String sourcePythonPath
    private final String sourcePythonBinary

    private boolean validateBinary = true
    private ContainerManager docker
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

    PythonBinary(Project project, String pythonPath, String binary) {
        this.project = project
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
            this.docker = DockerFactory.getContainer(docker, project)
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
     * @param sysExecutableProvider sys.executable value provider
     * @param sysPrefixProvider sys.prefix value provider
     * @return directory under python home containing python binary (always absolute path)
     * @see {@link ru.vyarus.gradle.plugin.python.cmd.Python#getBinaryDir()}
     */
    String getBinaryDir(Closure<String> sysExecutableProvider, Closure<String> sysPrefixProvider) {
        init()
        // use resolved executable to avoid incorrect resolution in case of venv inside venv
        String path = customBinaryPath ? project.file(executable).absolutePath : sysExecutableProvider.call()
        int idx = path.lastIndexOf(File.separator)

        String res
        if (path.empty || idx <= 0) {
            // just guess by home dir (yes, I know, this MIGHT be incorrect in some cases, but should be ok
            // for virtualenvs used in majority of cases)
            path = sysPrefixProvider.call()
            res = CliUtils.pythonBinPath(path)
        } else {
            // cut off binary
            res = path[0..idx - 1]
        }
        if (docker) {
            // in case of docker path might be either local or inside docker (depending on resolution method)
            // this way it would always be a docker path
            res = docker.toDockerPath(res)
        }
        return res
    }

    // may return null for docker if path is not located inside project dir!
    String getLocalPath(String path) {
        docker ? docker.toLocalPath(path) : path
    }

    boolean isWindows() {
        return docker ? docker.windows : Os.isFamily(Os.FAMILY_WINDOWS)
    }

    boolean isDocker() {
        return docker != null
    }

    // checks (absolute!) path for existence
    // method required for correct checking for files existence inside docker
    // LIMITED TO FILES
    boolean exists(String path) {
        init()
        if (docker) {
            // if path leads inside project then it might be checked locally
            String localPath = docker.toLocalPath(path)
            if (localPath != null) {
                return new File(localPath).exists()
            }
            // testing path inside docker
            String res = rawExec([windows ? "IF EXIST \"$path\" ECHO exists"
                                          : "test -f $path && echo \"exists\""] as String[])
            return res == 'exists'
        }
        return new File(path).exists()
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
        project.logger.info('Python arguments: {}', cmdArgs)

        String[] cmd = [exec]
        cmd += cmdArgs
        if (docker) {
            // change paths (according to docker mapping)
            // performed here in order to see actual command in logs and error message, plus, cleanups applied here
            docker.convertCommand(cmd)
            // called here to show container start/stop logs before executed command
            prepareEnvironment()
        }

        String commandLine = cmd.join(SPACE)
        String formattedCommand = cleanLoggedCommand(commandLine.replace('\r', '').replace(NL, SPACE))
        project.logger.log(logLevel, "[python] $formattedCommand")

        long start = System.currentTimeMillis()
        int res = rawExec(cmd, outputConsumer)
        project.logger.info('Python execution time: {}',
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
                this.executable = findPython(project, sourcePythonPath, sourcePythonBinary, validateBinary)
                // direct executable must be called with cmd
                // (https://docs.gradle.org/4.1/dsl/org.gradle.api.tasks.Exec.html)
                this.withCmd = sourcePythonPath && windows
                // custom python path used (which may be relative and conflict with workDir)
                this.customBinaryPath = sourcePythonPath as boolean
                this.initialized = true
            }
        }
    }

    @Memoized
    private static String getPythonBinary(String pythonPath, String binary, boolean python3Available, boolean windows) {
        String res = binary ?: 'python'
        if (pythonPath) {
            String path = pythonPath + (pythonPath.endsWith(File.separator) ? '' : File.separator)
            // $pythonPath/$binaryName(.exe)
            res = windows ? "${path}${res}.exe" : "$path$res"
        } else if (!binary && python3Available) {
            res = PYTHON3
        }
        return res
    }

    // cached (statically) to prevent multiple fs lookups because multiple Python objects would be constructed
    @Memoized
    private static File findSystemBinary(String binary) {
        // manually searching in system path to point to possibly incorrect PATH variable used in process
        // (might happen when process not started from user shell)
        return CliUtils.searchSystemBinary(binary)
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

    private String findPython(Project project, String pythonPath, String binary, boolean validateBinary) {
        String normalizedPath = pythonPath == null ? null : Paths.get(pythonPath).normalize().toString()
        // detecting python3 binary only if default python usage assumed (to avoid redundant python calls)
        boolean python3found = (binary || pythonPath) ? false : detectPython3Binary(project)
        String executable = getPythonBinary(normalizedPath, binary, python3found, windows)
        // search not performed for docker!
        if (validateBinary && !pythonPath && !docker) {
            // search would fail if no binary found in system paths
            project.logger.info('Found python binary: {}', findSystemBinary(executable).absolutePath)
        }
        return executable
    }

    // note: @Memoized not used because it would store link to Project object which could lead to significant
    // memory leak. And that's why this check is performed outside of getPythonBinary method
    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('SynchronizedMethod')
    private boolean detectPython3Binary(Project project) {
        synchronized (SYNC) {
            // root project property used for cache execution result in multi-module project
            if (project.rootProject.findProperty(PROP_PYTHON3) == null) {
                if (windows) {
                    project.rootProject.extensions.extraProperties.set(PROP_PYTHON3, false)
                } else {
                    project.rootProject.extensions.extraProperties.set(PROP_PYTHON3,
                            rawExec([PYTHON3, '--version'] as String[]) != null)
                }
            }
            return project.rootProject.findProperty(PROP_PYTHON3)
        }
    }

    private String cleanLoggedCommand(String cmd) {
        String res = cmd
        logCleaners.each { res = it.clear(cmd) }
        return res
    }

    private String reformatOutputForException(OutputStream os) {
        os.flush()
        String out = os
        return out ? out.split(NL).collect { '\t\t' + it }.join(NL) + NL : ''
    }

    private void prepareEnvironment() {
        if (dockerConfig.exclusive) {
            project.logger.lifecycle("[docker] executing command in exclusive container '{}' \n{}",
                    dockerConfig.image, docker.formatContainerInfo(dockerConfig, workDir, envVars))
        } else {
            // first executed command will start container and all subsequent calls would use already
            // started container
            docker.restartIfRequired(dockerConfig, workDir, envVars)
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
        if (docker) {
            // required here for direct raw execution case (normally, start logs must appear before executed command)
            if (!docker.started) {
                docker.restartIfRequired(dockerConfig, workDir, envVars)
            }
            return dockerConfig.exclusive
                    ? docker.execExclusive(command, out, dockerConfig, workDir, envVars)
                    : docker.exec(command, out)
        }
        ExecResult res = project.exec new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                spec.commandLine command
                spec.standardOutput = out
                spec.errorOutput = out
                spec.ignoreExitValue = true
                if (workDir) {
                    spec.setWorkingDir(workDir)
                }
                if (envVars) {
                    spec.environment(envVars)
                }
            }
        }
        return res.exitValue
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
                : (wrkDirUsed && customBinaryPath ? CliUtils.canonicalPath(project.file(executable)) : executable)
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
                .wincmdArgs(executable, project.projectDir, prepareArgs(args, pythonArgs, extraArgs), wrkDirUsed)
                : prepareArgs(args, pythonArgs, extraArgs)
    }

    private void beforeInit(String message) {
        if (initialized) {
            throw new IllegalStateException(message)
        }
    }
}
