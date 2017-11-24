package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.task.PipInstallTask
import ru.vyarus.gradle.plugin.python.task.PythonTask

/**
 * Use-python plugin. Plugin requires python installed globally or configured path to python binary.
 * <p>
 * Used to install required pip modules or revert installed versions if older required with {@code pipInstall} task
 * (guarantee exact modules versions). And use python modules, scripts, commands during gradle build
 * with {@link PythonTask}.
 * <p>
 * Also, plugin may be used as a base for building gradle plugin for specific python module.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PythonPlugin implements Plugin<Project> {

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void apply(Project project) {
        PythonExtension extension = project.extensions.create('python', PythonExtension)

        // simplify direct tasks usage
        project.extensions.extraProperties.set(PipInstallTask.simpleName, PipInstallTask)
        project.extensions.extraProperties.set(PythonTask.simpleName, PythonTask)

        // default pip install task
        PipInstallTask installTask = project.tasks.create('pipInstall', PipInstallTask) {
            description = 'Install pip modules'
            conventionMapping.modules = { extension.modules }
        }

        // apply defaults for all python tasks
        project.tasks.withType(PythonTask) { task ->
            task.conventionMapping.pythonPath = { extension.pythonPath }
            // by default all python tasks must be executed after dependencies init
            task.dependsOn installTask
        }

        // apply defaults for all pip install tasks (custom pip installs may be used)
        project.tasks.withType(PipInstallTask) { task ->
            task.conventionMapping.with {
                minPythonVersion = { extension.minVersion }
                pythonPath = { extension.pythonPath }
                showInstalledVersions = { extension.showInstalledVersions }
                alwaysInstallModules = { extension.alwaysInstallModules }
            }
        }
    }
}
