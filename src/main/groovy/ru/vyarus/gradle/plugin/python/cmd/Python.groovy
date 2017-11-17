package ru.vyarus.gradle.plugin.python.cmd

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecResult
import ru.vyarus.gradle.plugin.python.util.OutputLinesInterceptor
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
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

    Python workDir(String workDir) {
        if (workDir) {
            this.workDir = workDir
        }
        return this
    }

    Python outputPrefix(String prefix) {
        this.outputPrefix = prefix
        return this
    }

    Python logLevel(LogLevel level) {
        if (level) {
            this.logLevel = level
        }
        return this
    }

    Python extraArgs(Object args) {
        if (args) {
            this.extraArgs.addAll(Arrays.asList(convertArgs(args)))
        }
        return this
    }

    Python clearArgs() {
        this.extraArgs.clear()
        return this
    }

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

    void exec(Object args) {
        new OutputLinesInterceptor({ String line ->
            project.logger.log(logLevel, outputPrefix ? "$outputPrefix $line" : line)
        }).withStream { processExecution(args, it) }
    }

    void callModule(String module, Object args) {
        exec(mergeArgs("-m $module", args))
    }

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
        output.readLines().collect({ "$outputPrefix $it" }).join('\n')
    }
}
