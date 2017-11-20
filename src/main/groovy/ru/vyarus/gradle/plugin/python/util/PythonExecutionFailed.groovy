package ru.vyarus.gradle.plugin.python.util

import groovy.transform.CompileStatic
import org.gradle.api.GradleException

/**
 * Thrown when python command execution failed. Message will contain entire command.
 *
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
@CompileStatic
class PythonExecutionFailed extends GradleException {

    PythonExecutionFailed(String message) {
        super(message)
    }
}
