package ru.vyarus.gradle.plugin.python.task.env

import groovy.transform.CompileStatic

/**
 * Exception indicate required fallback to virtualenv tool (e.g. when venv is not installed).
 *
 * @author Vyacheslav Rusakov
 * @since 01.04.2024
 */
@CompileStatic
class FallbackException extends RuntimeException {
}
