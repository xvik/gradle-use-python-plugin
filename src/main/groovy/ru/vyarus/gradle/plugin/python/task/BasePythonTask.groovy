package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.internal.ConventionTask
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

    protected BasePythonTask() {
        group = 'python'
    }

    @Internal
    @Memoized
    protected Python getPython() {
        new Python(project, getPythonPath())
    }
}
