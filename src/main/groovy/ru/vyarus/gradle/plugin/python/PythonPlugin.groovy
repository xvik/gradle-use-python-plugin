package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import ru.vyarus.gradle.plugin.python.task.BasePythonTask
import ru.vyarus.gradle.plugin.python.task.CheckPythonTask
import ru.vyarus.gradle.plugin.python.task.PythonTask
import ru.vyarus.gradle.plugin.python.task.pip.BasePipTask
import ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask
import ru.vyarus.gradle.plugin.python.task.pip.PipListTask
import ru.vyarus.gradle.plugin.python.task.pip.PipUpdatesTask

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
        PythonExtension extension = project.extensions.create('python', PythonExtension, project)

        // simplify direct tasks usage
        project.extensions.extraProperties.set(PipInstallTask.simpleName, PipInstallTask)
        project.extensions.extraProperties.set(PythonTask.simpleName, PythonTask)
        // configuration shortcut
        PythonExtension.Scope.values().each { project.extensions.extraProperties.set(it.name(), it) }

        // validate installed python
        TaskProvider<CheckPythonTask> checkTask = project.tasks.register('checkPython', CheckPythonTask) {
            it.with {
                description = 'Validate python environment'
            }
        }

        // default pip install task
        TaskProvider<PipInstallTask> installTask = project.tasks.register('pipInstall', PipInstallTask) {
            it.with {
                description = 'Install pip modules'
            }
        }

        project.tasks.register('pipUpdates', PipUpdatesTask) {
            it.with {
                description = 'Check if new versions available for declared pip modules'
            }
        }

        project.tasks.register('pipList', PipListTask) {
            it.with {
                description = 'Show all installed modules'
            }
        }

        configureDefaults(project, extension, checkTask, installTask)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureDefaults(Project project,
                                   PythonExtension extension,
                                   TaskProvider<CheckPythonTask> checkTask,
                                   TaskProvider<PipInstallTask> installTask) {

        project.tasks.withType(BasePythonTask).configureEach { task ->
            task.with {
                // apply default path for all python tasks
                task.conventionMapping.with {
                    pythonPath = { extension.pythonPath }
                    pythonBinary = { extension.pythonBinary }
                }
                // all python tasks must be executed after check task to use correct environment (switch to virtualenv)
                if (task.taskIdentity.type != CheckPythonTask) {
                    dependsOn checkTask
                }
            }
        }

        project.tasks.withType(PythonTask).configureEach { task ->
            // by default all python tasks must be executed after dependencies init
            task.dependsOn installTask
        }

        // apply defaults for pip tasks
        project.tasks.withType(BasePipTask).configureEach { task ->
            task.conventionMapping.with {
                modules = { extension.modules }
                // in case of virtualenv checkPython will manually disable it
                userScope = { extension.scope != PythonExtension.Scope.GLOBAL }
                useCache = { extension.usePipCache }
                trustedHost = { extension.trustedHost }
                extraIndexUrl = { extension.extraIndexUrl }
            }
        }

        // apply defaults for all pip install tasks (custom pip installs may be used)
        project.tasks.withType(PipInstallTask).configureEach { task ->
            task.conventionMapping.with {
                showInstalledVersions = { extension.showInstalledVersions }
                alwaysInstallModules = { extension.alwaysInstallModules }
            }
        }
    }
}
