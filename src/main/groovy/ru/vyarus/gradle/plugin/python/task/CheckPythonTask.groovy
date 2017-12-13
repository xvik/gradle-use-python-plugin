package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecException
import ru.vyarus.gradle.plugin.python.PythonExtension
import ru.vyarus.gradle.plugin.python.task.pip.BasePipTask
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * Task validates python installation (will fail if python or pip not found or minimal version doesn't match).
 * Task called before any {@link ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask}
 * (by default, before pipInstall).
 *
 * @author Vyacheslav Rusakov
 * @since 08.12.2017
 */
@CompileStatic
class CheckPythonTask extends BasePipTask {

    @TaskAction
    @SuppressWarnings('UnnecessaryGetter')
    void run() {
        PythonExtension ext = project.extensions.findByType(PythonExtension)

        checkPythonInstalled()
        checkPythonVersion(ext)

        if (!getModules().empty) {
            checkPipInstalled()
            checkPipVersion(ext)
        }
    }

    private void checkPythonInstalled() {
        try {
            python.exec('--version')
        } catch (ExecException ex) {
            throw new GradleException("Python not found: $python.usedBinary. " +
                    'Please install it (http://docs.python-guide.org/en/latest/starting/installation/) ' +
                    'or configure correct location with \'python.pythonPath\'.', ex)
        }
    }

    private void checkPythonVersion(PythonExtension ext) {
        String version = python.version
        String minVersion = ext.minPythonVersion
        if (!CliUtils.isVersionMatch(version, minVersion)) {
            throw new GradleException("Python ($python.homeDir) verion $version does not match minimal " +
                    "required version: $minVersion")
        }
        logger.lifecycle('Using python {} from {} ({})', python.version, python.homeDir, python.usedBinary)
    }

    private void checkPipInstalled() {
        try {
            pip.versionLine
        } catch (PythonExecutionFailed ex) {
            throw new GradleException('Pip is not installed. Please install it ' +
                    '(https://pip.pypa.io/en/stable/installing/).', ex)
        }
    }

    private void checkPipVersion(PythonExtension ext) {
        String version = pip.version
        String minVersion = ext.minPipVersion
        if (!CliUtils.isVersionMatch(version, minVersion)) {
            throw new GradleException("Pip verion $version does not match minimal " +
                    "required version: $minVersion. Use 'pip install -U pip' to upgrade pip.")
        }
        logger.lifecycle('Using {}', pip.versionLine)
    }
}
