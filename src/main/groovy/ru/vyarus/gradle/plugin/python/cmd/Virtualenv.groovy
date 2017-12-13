package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

/**
 * Pip commands execution utility. Use {@link Python} internally.
 *
 * @author Vyacheslav Rusakov
 * @since 13.12.2017
 */
@CompileStatic
class Virtualenv {

    private final Python python

    final String path
    final File location

    Virtualenv(Project project, String path) {
        this(project, null, null, path)
    }

    Virtualenv(Project project, String pythonPath, String binary, String path) {
        python = new Python(project, pythonPath, binary)
                .logLevel(LogLevel.LIFECYCLE)
        this.path = path
        if (!path) {
            throw new IllegalArgumentException('Virtualenv path not set')
        }
        location = project.file(path)
    }

    /**
     * @return virtualenv version (major.minor.micro)
     */
    @Memoized
    String getVersion() {
        python.withHiddenLog {
            python.readOutput('-m virtualenv --version')
        }
    }

    /**
     * @return true if virtualenv exists
     */
    boolean exists() {
        return location.exists() && location.list().size() > 0
    }

    /**
     * Create virtualenv. Do nothing if already exists.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void create() {
        if (exists()) {
            return
        }
        python.callModule('virtualenv', path)
    }
}
