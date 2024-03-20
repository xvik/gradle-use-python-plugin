package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
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
     * Create work directory if it doesn't exist. Enabled by default.
     */
    @Input
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
     * Command could be specified as string, array or list (iterable).
     */
    @Input
    @Optional
    Object command

    /**
     * Prefix each line of python output. By default it's '\t' to indicate command output.
     */
    @Input
    @Optional
    String outputPrefix = '\t'

    /**
     * Extra arguments to append to every called command.
     * Useful for pre-configured options, applied to all executed commands
     * <p>
     * Option not available in {@link BasePythonTask} because of pip tasks which use different set of keys
     * for various commands. Special pip tasks like {@link ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask}
     * use multiple different calls internally and general extra args would apply to all of them and, most likely,
     * crash the build. It is better to implement external arguments support on exact task level (to properly apply it
     * to exact executed command and avoid usage confusion).
     */
    @Input
    @Optional
    List<String> extraArgs = []

    @TaskAction
    void run() {
        String mod = getModule()
        Object cmd = getCommand()
        if (!mod && !cmd) {
            throw new GradleException('Module or command to execute must be defined')
        }
        initWorkDirIfRequired()

        Python python = python
                .outputPrefix(getOutputPrefix())
                .extraArgs(getExtraArgs())

        // task-specific logger required for exclusive docker usage, because otherwise project logger would
        // show output below previous task (in exclusive mode logs would come from separate thread)
        if (mod) {
            python.callModule(logger, mod, cmd)
        } else {
            python.exec(logger, cmd)
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

    @SuppressWarnings('UnnecessaryGetter')
    private void initWorkDirIfRequired() {
        String dir = getWorkDir()
        if (dir && isCreateWorkDir()) {
            File wrkd = gradleEnv.file(dir)
            if (!wrkd.exists()) {
                wrkd.mkdirs()
            }
        }
    }
}
