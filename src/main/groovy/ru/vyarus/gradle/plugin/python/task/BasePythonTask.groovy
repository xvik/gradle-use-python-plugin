package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import ru.vyarus.gradle.plugin.python.cmd.Python

/**
 * Base task for all python tasks.
 *
 * @author Vyacheslav Rusakov
 * @since 01.12.2017
 */
@CompileStatic
class BasePythonTask extends ConventionTask {

    /**
     * Path to directory with python executable. Not required if python installed globally.
     * Automatically set from {@link ru.vyarus.gradle.plugin.python.PythonExtension#pythonPath}, but could
     * be overridden manually.
     */
    @Input
    @Optional
    String pythonPath

    /**
     * Python binary name. When empty: use python3 or python for linux and python for windows.
     * Automatically set from {@link ru.vyarus.gradle.plugin.python.PythonExtension#pythonBinary}, but could
     * be overridden manually.
     */
    @Input
    @Optional
    String pythonBinary

    /**
     * Python arguments applied to all executed commands. Arguments applied before called command
     * (and so option may be useful for cases impossible with {@link PythonTask#extraArgs}, applied after command).
     * For example, it could be used for -I or -S flags (be aware that -S can cause side effects, especially
     * inside virtual environments).
     */
    @Input
    @Optional
    List<String> pythonArgs = []

    /**
     * Environment variables for executed python process (variables specified in gradle's
     * {@link org.gradle.process.ExecSpec#environment(java.util.Map)} during python process execution).
     */
    @Input
    @Optional
    Map<String, Object> environment = [:]

    /**
     * Working directory. Not required, but could be useful for some modules (e.g. generators).
     */
    @Input
    @Optional
    String workDir

    /**
     * Python logs output level. By default it's {@link org.gradle.api.logging.LogLevel@LIFECYCLE}
     * (visible with '-i' gradle flag).
     */
    @Input
    @Optional
    LogLevel logLevel = LogLevel.LIFECYCLE

    protected BasePythonTask() {
        group = 'python'
    }

    /**
     * Add python arguments, applied before command.
     *
     * @param args arguments
     */
    @SuppressWarnings('ConfusingMethodName')
    void pythonArgs(String... args) {
        if (args) {
            getPythonArgs().addAll(args)
        }
    }

    /**
     * Add environment variable for python process (will override previously set value).
     *
     * @param var variable name
     * @param value variable value
     */
    @SuppressWarnings('ConfusingMethodName')
    void environment(String var, Object value) {
        getEnvironment().put(var, value)
    }

    /**
     * Add environment variables for python process (will override already set values, but not replace context
     * map completely). May be called multiple times: all variables would be aggregated.
     *
     * @param vars (may be null)
     */
    @SuppressWarnings('ConfusingMethodName')
    void environment(Map<String, Object> vars) {
        if (vars) {
            getEnvironment().putAll(vars)
        }
    }

    @Internal
    @Memoized
    protected Python getPython() {
        new Python(project, getPythonPath(), getPythonBinary())
                .logLevel(getLogLevel())
                .workDir(getWorkDir())
                .pythonArgs(getPythonArgs())
                .environment(environment)
    }
}
