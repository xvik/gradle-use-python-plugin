package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import ru.vyarus.gradle.plugin.python.service.EnvService
import ru.vyarus.gradle.plugin.python.task.BasePythonTask
import ru.vyarus.gradle.plugin.python.task.CheckPythonTask
import ru.vyarus.gradle.plugin.python.task.PythonTask
import ru.vyarus.gradle.plugin.python.task.pip.BasePipTask
import ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask
import ru.vyarus.gradle.plugin.python.task.pip.PipListTask
import ru.vyarus.gradle.plugin.python.task.pip.PipUpdatesTask
import ru.vyarus.gradle.plugin.python.util.RequirementsReader

/**
 * Use-python plugin. Plugin requires python installed globally or configured path to python binary.
 * Alternatively, docker might be used.
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
@SuppressWarnings('DuplicateStringLiteral')
class PythonPlugin implements Plugin<Project> {

    private Provider<EnvService> envService

    @Override
    void apply(Project project) {
        PythonExtension extension = project.extensions.create('python', PythonExtension, project)

        initService(project, extension)

        // simplify direct tasks usage
        project.extensions.extraProperties.set(PipInstallTask.simpleName, PipInstallTask)
        project.extensions.extraProperties.set(PythonTask.simpleName, PythonTask)
        // configuration shortcut
        PythonExtension.Scope.values().each { project.extensions.extraProperties.set(it.name(), it) }

        createTasks(project, extension)
    }

    private void initService(Project project, PythonExtension extension) {
        // service used to shutdown docker properly and hold actual python path link
        envService = project.gradle.sharedServices.registerIfAbsent(
                'pythonEnvironmentService', EnvService, spec -> { })

        // can't use service properties because each project in multi-module project must have unique path
        envService.get().defaultProvider(project.path, { extension.pythonPath } as Provider<String>)
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    private void createTasks(Project project, PythonExtension extension) {
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

        project.tasks.register('cleanPython', Delete) {
            it.with {
                group = 'python'
                description = 'Removes existing python environment (virtualenv)'
                delete extension.envPath
                onlyIf { project.file(extension.envPath).exists() }
            }
        }

        configureDefaults(project, extension, checkTask, installTask)
    }

    @SuppressWarnings(['MethodSize', 'AbcMetric'])
    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureDefaults(Project project,
                                   PythonExtension extension,
                                   TaskProvider<CheckPythonTask> checkTask,
                                   TaskProvider<PipInstallTask> installTask) {

        project.tasks.withType(BasePythonTask).configureEach { task ->
            task.with {
                // apply default path for all python tasks
                task.conventionMapping.with {
                    // IMPORTANT: pythonPath might change after checkPythonTask (switched to environment)
                    pythonPath = { envService.get().getPythonPath(project.path) }
                    pythonBinary = { extension.pythonBinary }
                    validateSystemBinary = { extension.validateSystemBinary }
                    // important to copy map because each task must have independent instance
                    environment = { extension.environment ? new HashMap<>(extension.environment) : null }
                }

                // can't be implemented with convention mapping, only with properties
                configureDockerInTask(project, extension.docker, task)

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
                trustedHosts = { extension.trustedHosts }
                extraIndexUrls = { extension.extraIndexUrls }
                requirements = { RequirementsReader.find(task.gradleEnv, extension.requirements) }
                strictRequirements = { extension.requirements.strict }
            }
        }

        // apply defaults for all pip install tasks (custom pip installs may be used)
        project.tasks.withType(PipInstallTask).configureEach { task ->
            task.conventionMapping.with {
                showInstalledVersions = { extension.showInstalledVersions }
                alwaysInstallModules = { extension.alwaysInstallModules }
                envPath = { extension.envPath }
            }
        }

        project.tasks.withType(CheckPythonTask).configureEach { task ->
            task.envService = this.envService
            task.conventionMapping.with {
                scope = { extension.scope }
                envPath = { extension.envPath }
                minPythonVersion = { extension.minPythonVersion }
                minPipVersion = { extension.minPipVersion }
                installVirtualenv = { extension.installVirtualenv }
                virtualenvVersion = { extension.virtualenvVersion }
                minVirtualenvVersion = { extension.minVirtualenvVersion }
                envCopy = { extension.envCopy }
            }
        }
    }

    private void configureDockerInTask(Project project, PythonExtension.Docker docker, BasePythonTask task) {
        task.docker.use.convention(project.provider { docker.use })
        task.docker.image.convention(project.provider { docker.image })
        task.docker.windows.convention(project.provider { docker.windows })
        task.docker.ports.convention(project.provider { docker.ports })
        task.docker.exclusive.convention(false)
    }
}
