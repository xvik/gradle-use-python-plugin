package ru.vyarus.gradle.plugin.python.util

import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecException

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class ExecRes implements ExecResult {

    int returnValue

    ExecRes(int returnValue) {
        this.returnValue = returnValue
    }

    @Override
    int getExitValue() {
        return returnValue
    }

    @Override
    ExecResult assertNormalExitValue() throws ExecException {
        return null
    }

    @Override
    ExecResult rethrowFailure() throws ExecException {
        return null
    }
}
