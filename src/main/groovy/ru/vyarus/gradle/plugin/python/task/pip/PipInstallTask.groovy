package ru.vyarus.gradle.plugin.python.task.pip

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Install required modules (with correct versions) into python using pip.
 * Default task is registered as pipInstall to install all modules declared in
 * {@link ru.vyarus.gradle.plugin.python.PythonExtension#modules}.
 * All {@link ru.vyarus.gradle.plugin.python.task.PythonTask}s are depend on pipInstall by default.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PipInstallTask extends BasePipTask {

    /**
     * True to show list of all installed python modules (not only modules installed by plugin!).
     * By default use {@link ru.vyarus.gradle.plugin.python.PythonExtension#showInstalledVersions} value.
     */
    @Console
    boolean showInstalledVersions
    /**
     * True to always call 'pip install' for configured modules, otherwise pip install called only
     * if module is not installed or different version installed.
     * By default use {@link ru.vyarus.gradle.plugin.python.PythonExtension#showInstalledVersions} value.
     */
    @Input
    @Optional
    boolean alwaysInstallModules

    @TaskAction
    void run() {
        validatePython()

        if (!modulesList.isEmpty()) {
            // use list of installed modules to check if 'pip install' is required for module
            List<String> installed = (isAlwaysInstallModules()
                    ? '' : python.readOutput('-m pip freeze')).toLowerCase().readLines()
            boolean altered = false
            // install modules
            modulesList.each { PipModule mod ->
                String pipDef = mod.toPipString()
                // don't install if already installed (assume dependencies are also installed)
                if (!installed.contains(pipDef.toLowerCase())) {
                    pip.install(pipDef)
                    altered = true
                }
            }
            if (!altered) {
                logger.lifecycle('All required modules are already installed with correct versions')
            }
            // only if there are any required modules, otherwise no need
            if (isShowInstalledVersions()) {
                // show all installed modules versions (to help problems resolution)
                pip.exec('list --format=columns')
            }
        }
    }
}
