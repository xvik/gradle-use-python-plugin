package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecResult
import ru.vyarus.gradle.plugin.python.util.CliUtils
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

    private static final String PYTHON = 'python'
    private static final String PYTHON3 = 'python3'
    private static final String SPACE = ' '

    private final Project project
    private final String executable
    private String workDir
    private String outputPrefix = '\t'
    private LogLevel logLevel = LogLevel.INFO
    private final List<String> extraArgs = []

    Python(Project project) {
        this(project, null, null)
    }

    Python(Project project, String pythonPath, String binary) {
        this.project = project
        this.executable = getPythonBinary(project, pythonPath, binary)
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
        return readOutput('-c "import sys;print(sys.prefix)"')
    }

    /**
     * @return python version in format major.minor.micro
     */
    @Memoized
    String getVersion() {
        return readOutput('-c "import sys;ver = sys.version_info;print(' +
                'str(ver.major)+\'.\'+str(ver.minor)+\'.\'+str(ver.micro))"')
    }

    @SuppressWarnings('UnnecessarySetter')
    private void processExecution(Object args, OutputStream os) {
        String[] cmd = CliUtils.parseArgs(args)
        if (this.extraArgs) {
            cmd = CliUtils.mergeArgs(cmd, extraArgs)
        }
        String commandLine = "$executable ${cmd.join(SPACE)}"
        String formattedCommand = commandLine.replace('\r', '').replace('\n', SPACE)
        project.logger.log(logLevel, "[python] $formattedCommand")

        ExecResult res = project.exec {
            it.executable = this.executable
            it.args(cmd)
            standardOutput = os
            errorOutput = os
            ignoreExitValue = true
            if (workDir) {
                setWorkingDir(workDir)
            }
        }
        if (res.exitValue != 0) {
            throw new PythonExecutionFailed("Python call failed: $formattedCommand")
        }
    }

    @Memoized
    private static String getPythonBinary(Project project, String pythonPath, String binary) {
        String res = binary ?: PYTHON
        boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
        if (pythonPath) {
            res = pythonPath + (pythonPath.endsWith('/') ? '' : '/') + PYTHON + (isWindows ? '.exe' : '')
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
}
