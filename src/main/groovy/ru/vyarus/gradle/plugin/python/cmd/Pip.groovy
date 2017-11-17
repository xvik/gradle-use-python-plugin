package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

/**
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
class Pip {

    private final Python python

    Pip(Project project, String pythonPath) {
        python = new Python(project, pythonPath)
                .logLevel(LogLevel.LIFECYCLE)
    }

    void install(String module) {
        exec("install $module")
    }

    void exec(String cmd) {
        python.callModule('pip', cmd)
    }

    Python getPython() {
        return python
    }
}
