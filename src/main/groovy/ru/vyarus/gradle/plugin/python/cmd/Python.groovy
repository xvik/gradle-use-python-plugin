package ru.vyarus.gradle.plugin.python.cmd

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecResult
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
class Python {

    private static final String[] EMPTY = []

    private Project project
    private String executable
    private String workDir
    private String outputPrefix = '\t'
    private LogLevel logLevel = LogLevel.INFO
    private List<String> extraArgs = []

    Python(Project project, String pythonPath) {
        this.project = project
        this.executable = getPythonBinary(pythonPath)
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
     * @param level gradle log level to use for python output
     * @return cli instance for chained calls
     */
    Python logLevel(LogLevel level) {
        if (level) {
            this.logLevel = level
        }
        return this
    }

    /**
     * Useful if all called commands support common keys (usually this mean one module usage).
     * Arguments are appended. To cleat existing arguments see {@link #clearExtraArgs()}.
     *
     * @param args extra arguments to apply to all processed commands
     * @return cli instance for chained calls
     */
    Python extraArgs(Object args) {
        if (args) {
            this.extraArgs.addAll(Arrays.asList(convertArgs(args)))
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
    String readOutput(Object args) {
        return new ByteArrayOutputStream().withStream { os ->
            try {
                processExecution(args, os)
                return os.toString().trim()
            } catch (Throwable th) {
                // print process output, because it might contain important error details
                def output = os.toString().trim()
                if (output) {
                    project.logger.error(prefixOutput(output))
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
        exec(mergeArgs("-m $module", args))
    }

    /**
     * @return python home directory (works for global python too)
     */
    String getHomeDir() {
        return readOutput('-c "import sys;\nprint(sys.prefix)"')
    }

    private processExecution(Object args, OutputStream os) {
        String[] cmd = convertArgs(args)
        if (this.extraArgs) {
            cmd = mergeArgs(cmd, extraArgs)
        }
        String commandLine = "$executable ${cmd.join(' ')}"
        // prefix backslashes for prettier tostring
        project.logger.log(logLevel,
                "[python] ${commandLine.replace('\\', '\\\\')}")

        ExecResult res = project.exec {
            executable = this.executable
            it.args(cmd)
            standardOutput = os
            errorOutput = os
            ignoreExitValue = true
            if (workDir) {
                workingDir = workDir
            }
        }
        if (res.exitValue != 0) {
            throw new PythonExecutionFailed("Python call failed: $commandLine")
        }
    }


    private String getPythonBinary(String pythonPath) {
        String res = 'python'
        if (pythonPath) {
            boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
            res = pythonPath + (pythonPath.endsWith('/') ? '' : '/') + 'python' + (isWindows ? '.exe' : '')
        }
        return res
    }

    private String[] mergeArgs(Object args1, Object args2) {
        String[] args = []
        args += convertArgs(args1)
        args += convertArgs(args2)
        return args
    }

    private String[] convertArgs(Object args) {
        String[] res = EMPTY
        if (args) {
            if (args instanceof String || args instanceof GString) {
                res = parseCommandLine(args.toString())
            } else {
                res = args as String[]
            }
        }
        return res
    }

    private String[] parseCommandLine(String command) {
        String cmd = command.trim()
        return cmd ? cmd
                .replaceAll('\\s{2,}', ' ')
                .split(' ')
                : EMPTY
    }

    private String prefixOutput(String output) {
        outputPrefix ? output.readLines().collect({ "$outputPrefix $it" }).join('\n') : output
    }
}
