package ru.vyarus.gradle.plugin.python.util

import org.gradle.api.GradleException

/**
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
class PythonExecutionFailed extends GradleException {

    PythonExecutionFailed(String message) {
        super(message)
    }
}
