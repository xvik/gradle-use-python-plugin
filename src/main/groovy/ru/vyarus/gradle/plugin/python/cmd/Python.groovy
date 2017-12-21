package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecResult
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.DurationFormatter
import ru.vyarus.gradle.plugin.python.util.OutputLogger
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * Python commands execution utility. Use global python or binary in provided python path.
 * <p>
 * Usage: configure instance with builder style configuration
 * methods and then execute commands. Command output may be redirected to gradle logger (with configurable
 * level and prefix) or returned as result.
 * <p>
 * Arguments may be defined as:
 * <ul>
 *     <li>array or collection {@code ['arg', 'arg']}</li>
 *     <li>pure string {@code 'arg arg --somekey'}</li>
 * </ul>
 * <p>
 * See {@link Pip} as example of usage for package specific utility.
 *
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
@CompileStatic
@SuppressWarnings('ConfusingMethodName')
class Python {

    private static final String PYTHON3 = 'python3'
    private static final String SPACE = ' '
    private static final String SLASH = '/'

    private final Project project
    private final String executable
    private String workDir
    private String outputPrefix = '\t'
    private LogLevel logLevel = LogLevel.INFO
    private final List<String> extraArgs = []

    // run through cmd on win (when direct executable called)
    private final boolean withCmd
    // set when calling custom binary by path instead of global (required to rewrite path to absolute)
    private final boolean customBinaryPath

    Python(Project project) {
        this(project, null, null)
    }

    Python(Project project, String pythonPath, String binary) {
        this.project = project
        this.executable = getPythonBinary(project, pythonPath, binary)
        // direct executable must be called with cmd (https://docs.gradle.org/4.1/dsl/org.gradle.api.tasks.Exec.html)
        this.withCmd = pythonPath && Os.isFamily(Os.FAMILY_WINDOWS)
        // custom python path used (which may be relative and conflict with workDir)
        this.customBinaryPath = pythonPath as boolean
    }

    /**
     * @param workDir working directory (null value ignored for simplified usage)
     * @return cli instance for chained calls
     */
    Python workDir(String workDir) {
        if (workDir) {
            this.workDir = workDir
        }
        return this
    }

    /**
     * By default '\t' prefix used (to indent python command output).
     *
     * @param prefix prefix applied to python execution output (null mean no prefix)
     * @return cli instance for chained calls
     */
    Python outputPrefix(String prefix) {
        this.outputPrefix = prefix
        return this
    }

    /**
     * By default, {@link LogLevel#INFO} level is used (visible with gradle '-i' flag).
     * For example, {@link Pip} use {@link LogLevel#LIFECYCLE} because pip logs must be always visible.
     *
     * @param level gradle log level to use for python output (null value ignored for simplified usage)
     * @return cli instance for chained calls
     */
    Python logLevel(LogLevel level) {
        if (level != null) {
            this.logLevel = level
        }
        return this
    }

    /**
     * Useful if all called commands support common keys (usually this mean one module usage).
     * Arguments are appended. To cleat existing arguments see {@link #clearExtraArgs()}.
     *
     * @param args extra arguments (array, collection or simple string) to apply to all processed commands
     * (null value ignored for simplified usage)
     * @return cli instance for chained calls
     */
    Python extraArgs(Object args) {
        if (args) {
            this.extraArgs.addAll(Arrays.asList(CliUtils.parseArgs(args)))
        }
        return this
    }

    /**
     * Removes all registered extra arguments.
     *
     * @return cli instance for chained calls
     */
    Python clearExtraArgs() {
        this.extraArgs.clear()
        return this
    }

    /**
     * Execute python command and return all output (without applying the prefix!).
     * Very handy for short scripts evaluation. In case of error, all output is logged (with prefix).
     *
     * @param args command line arguments (array, collection or simple string)
     * @return python command execution output
     * @throws PythonExecutionFailed when process fails
     */
    @SuppressWarnings('CatchThrowable')
    String readOutput(Object args) {
        return new ByteArrayOutputStream().withStream { os ->
            try {
                processExecution(args, os)
                return os.toString().trim()
            } catch (Throwable th) {
                // print process output, because it might contain important error details
                String output = os.toString().trim()
                if (output) {
                    project.logger.error(CliUtils.prefixOutput(output, outputPrefix))
                }
                throw th
            }
        }
    }

    /**
     * Execute python command. All output redirected to gradle log (line by line).
     *
     * @param args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void exec(Object args) {
        new OutputLogger(project.logger, logLevel, outputPrefix).withStream { processExecution(args, it) }
    }

    /**
     * Calls command on module. Useful for integrations to avoid manual arguments merge for module.
     *
     * @param module called python module name
     * @param args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void callModule(String module, Object args) {
        exec(CliUtils.mergeArgs("-m $module", args))
    }

    /**
     * @return python home directory (works for global python too)
     */
    @Memoized
    String getHomeDir() {
        return resolveInfo()[1]
    }

    /**
     * @return python version in format major.minor.micro
     */
    @Memoized
    String getVersion() {
        return resolveInfo()[0]
    }

    /**
     * Used to call python instance methods with hidden (not visible in gradle console) log (set to INFO).
     *
     * @param closure closure with python commands to execute
     */
    public <T> T withHiddenLog(Closure closure) {
        LogLevel level = logLevel
        logLevel(LogLevel.INFO)
        try {
            return (T) closure.call()
        } finally {
            logLevel(level)
        }
    }

    /**
     * @return python binary used
     */
    String getUsedBinary() {
        this.executable
    }

    @SuppressWarnings('UnnecessarySetter')
    private void processExecution(Object args, OutputStream os) {
        boolean wrkDirUsed = workDir as boolean
        // on win non global python could be called only through cmd
        String exec = withCmd ? 'cmd'
                // use absolute python path if work dir set (relative will simply not work)
                : (wrkDirUsed && customBinaryPath ? project.file(executable).canonicalPath : executable)
        String[] cmd = withCmd ?
                CliUtils.wincmdArgs(executable, project.rootDir, prepareArgs(args), wrkDirUsed)
                : prepareArgs(args)

        String commandLine = "$exec ${cmd.join(SPACE)}"
        String formattedCommand = commandLine.replace('\r', '').replace('\n', SPACE)
        project.logger.log(logLevel, "[python] $formattedCommand")

        long start = System.currentTimeMillis()
        ExecResult res = project.exec {
            it.executable = exec
            it.args(cmd)
            standardOutput = os
            errorOutput = os
            ignoreExitValue = true
            if (workDir) {
                setWorkingDir(workDir)
            }
        }
        project.logger.info('Python execution time: {}',
                DurationFormatter.format(System.currentTimeMillis() - start))
        if (res.exitValue != 0) {
            throw new PythonExecutionFailed("Python call failed: $formattedCommand")
        }
    }

    @Memoized
    private static String getPythonBinary(Project project, String pythonPath, String binary) {
        String res = binary ?: 'python'
        boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
        if (pythonPath) {
            String path = pythonPath + (pythonPath.endsWith(SLASH) ? '' : SLASH)
            // $pythonPath/$binaryName(.exe)
            res = isWindows ? "${path.replace(SLASH, '\\')}${res}.exe" : "$path$res"
        } else if (!binary && !isWindows) {
            // check if python3 available
            new ByteArrayOutputStream().withStream { os ->
                ExecResult ret = project.exec {
                    standardOutput = os
                    errorOutput = os
                    ignoreExitValue = true
                    commandLine PYTHON3, '--version'
                }
                if (ret.exitValue == 0) {
                    res = PYTHON3
                }
            }
        }
        return res
    }

    @SuppressWarnings('Instanceof')
    private String[] prepareArgs(Object args) {
        String[] cmd = CliUtils.parseArgs(args)
        if (args instanceof CharSequence) {
            // important to easily spot args parsing problems
            project.logger.info("Parsed arguments line '{}': {}", args, cmd)
        }
        detectAndWrapCommand(cmd)
        if (this.extraArgs) {
            cmd = CliUtils.mergeArgs(cmd, extraArgs)
        }
        return cmd
    }

    /**
     * Detect python command call (-c) and wrap command argument if required (on linux).
     * @param cmd
     */
    private void detectAndWrapCommand(String[] cmd) {
        boolean moduleCall = false
        cmd.eachWithIndex { String arg, int i ->
            if (arg == '-m') {
                moduleCall = true
            }
            if (!moduleCall && arg == '-c' && i + 1 < cmd.length) {
                // wrap command to grant cross-platform compatibility (simple -c "string" is not always executed)
                cmd[i + 1] = CliUtils.wrapCommand(cmd[i + 1])
            }
        }
    }

    /**
     * Reduce the number of python executions during initialization.
     *
     * @return [raw python version, python home path]
     */
    @Memoized
    private List<String> resolveInfo() {
        String[] cmd = [
                'import sys',
                'ver=sys.version_info',
                'print(str(ver.major)+\'.\'+str(ver.minor)+\'.\'+str(ver.micro))',
                'print(sys.prefix)',
        ]
        withHiddenLog {
            // raw version, home path
            readOutput("-S -c \"${cmd.join(';')}\"")
                    .readLines()
        }
    }
}
