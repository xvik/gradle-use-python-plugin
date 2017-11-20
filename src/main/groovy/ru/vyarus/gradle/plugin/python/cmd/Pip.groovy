package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

/**
 * Pip commands execution utility. Use {@link Python} internally.
 *
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
@CompileStatic
class Pip {

    private final Python python

    Pip(Project project, String pythonPath) {
        python = new Python(project, pythonPath)
                .logLevel(LogLevel.LIFECYCLE)
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
     * Execute command on pip module. E.g. 'install some==12.3'.
     *
     * @param cmd pip command to execute
     */
    void exec(String cmd) {
        python.callModule('pip', cmd)
    }

    /**
     * May be used to change default configurations.
     *
     * @return python cli instance used to execute commands
     */
    Python getPython() {
        return python
    }
}
