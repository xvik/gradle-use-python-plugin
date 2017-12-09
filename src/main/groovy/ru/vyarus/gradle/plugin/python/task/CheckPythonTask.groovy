package ru.vyarus.gradle.plugin.python.task

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * Task validates python installation (will fail if python not found or minimal version doesn't match).
 * Task called before any {@link ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask}
 * (by default, before pipInstall).
 *
 * @author Vyacheslav Rusakov
 * @since 08.12.2017
 */
class CheckPythonTask extends BasePythonTask {

    /**
     * Minimal required python version.
     * By default use {@link ru.vyarus.gradle.plugin.python.PythonExtension#minVersion} value.
     */
    @Input
    @Optional
    String minPythonVersion

    @TaskAction
    void run() {
        String dir = python.homeDir
        String version = python.version
        String minVersion = getMinPythonVersion()
        if (!CliUtils.isVersionMatch(version, minVersion)) {
            throw new GradleException("Python ($dir) verion $version does not match minimal " +
                    "required version: $minVersion")
        }
        logger.lifecycle('Using Python {} ({})', version, dir)
    }
}
