package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.python.cmd.Python

/**
 * Task to execute python command (call module, script) using globally installed python.
 * All python tasks are called after default pipInstall task.
 * <p>
 * In essence, task duplicates {@link Python} utility configuration and use it for execution.
 * <p>
 * Task may be used as base class for specific modules tasks.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PythonTask extends BasePythonTask {

    /**
     * Working directory. Not required, but could be useful for some modules (e.g. generators).
     */
    @Input
    @Optional
    String workDir
    /**
     * Create work directory if it doesn't exist. Enabled by default.
     */
    @Input
    @Optional
    boolean createWorkDir = true
    /**
     * Module name. If specified, "-m module " will be prepended to specified command (if command not specified then
     * modules will be called directly).
     */
    @Input
    @Optional
    String module
    /**
     * Python command to execute. If module name set then it will be module specific command.
     * Examples:
     * <ul>
     * <li>direct module call: {@code '-m mod cmd'}
     * <li>code execution: {@code '-c import sys;\nsys...'}
     * <li>file execution: {@code 'path/to/file.py} (relative to workDir)
     * </ul>
     */
    @Input
    @Optional
    String command
    /**
     * Python logs output level. By default it's {@link LogLevel@LIFECYCLE} (visible with '-i' gradle flag).
     */
    @Input
    @Optional
    LogLevel logLevel = LogLevel.LIFECYCLE
    /**
     * Extra arguments to append to every called command.
     * Useful for pre-configured options, applied to all executed commands
     */
    @Input
    @Optional
    List<String> extraArgs = []
    /**
     * Prefix each line of python output. By default it's '\t' to indicate command output.
     */
    @Input
    @Optional
    String outputPrefix = '\t'

    @TaskAction
    void run() {
        String mod = getModule()
        String cmd = getCommand()
        if (!mod && !cmd) {
            throw new GradleException('Module or command to execute must be defined')
        }
        initWorkDirIfRequired()

        Python python = python
                .logLevel(getLogLevel())
                .outputPrefix(getOutputPrefix())
                .workDir(getWorkDir())
                .extraArgs(getExtraArgs())

        if (mod) {
            python.callModule(mod, cmd)
        } else {
            python.exec(cmd)
        }
    }

    /**
     * Add extra arguments, applied to command.
     *
     * @param args arguments
     */
    @SuppressWarnings('ConfusingMethodName')
    void extraArgs(String... args) {
        if (args) {
            getExtraArgs().addAll(args)
        }
    }

    private void initWorkDirIfRequired() {
        String dir = getWorkDir()
        if (dir && isCreateWorkDir()) {
            File docs = project.file(dir)
            if (!docs.exists()) {
                docs.mkdirs()
            }
        }
    }
}
