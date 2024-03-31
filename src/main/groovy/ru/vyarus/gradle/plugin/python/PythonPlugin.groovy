package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.build.event.BuildEventsListenerRegistry
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.cmd.env.GradleEnvironment
import ru.vyarus.gradle.plugin.python.service.EnvService
import ru.vyarus.gradle.plugin.python.task.BasePythonTask
import ru.vyarus.gradle.plugin.python.task.CheckPythonTask
import ru.vyarus.gradle.plugin.python.task.PythonTask
import ru.vyarus.gradle.plugin.python.task.pip.BasePipTask
import ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask
import ru.vyarus.gradle.plugin.python.task.pip.PipListTask
import ru.vyarus.gradle.plugin.python.task.pip.PipUpdatesTask
import ru.vyarus.gradle.plugin.python.util.RequirementsReader

import javax.inject.Inject

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
abstract class PythonPlugin implements Plugin<Project> {

    // need to be static and public for configuration cache support
    static void configureDockerInTask(Project project, PythonExtension.Docker docker, BasePythonTask task) {
        task.docker.use.convention(project.provider { docker.use })
        task.docker.image.convention(project.provider { docker.image })
        task.docker.windows.convention(project.provider { docker.windows })
        task.docker.useHostNetwork.convention(project.provider { docker.useHostNetwork })
        task.docker.ports.convention(project.provider { docker.ports })
        task.docker.exclusive.convention(false)
    }

    static PythonExtension findRootExtension(Project project) {
        PythonExtension rootExt = null
        Project prj = project
        while (prj != null) {
            PythonExtension cand = prj.extensions.findByType(PythonExtension)
            if (cand != null) {
                rootExt = cand
            }
            prj = prj.parent
        }
        return rootExt
    }

    @Inject
    abstract BuildEventsListenerRegistry getEventsListenerRegistry()

    @Override
    void apply(Project project) {
        PythonExtension extension = project.extensions.create('python', PythonExtension, project)

        Provider<EnvService> envService = initService(project)

        // simplify direct tasks usage
        project.extensions.extraProperties.set(PipInstallTask.simpleName, PipInstallTask)
        project.extensions.extraProperties.set(PythonTask.simpleName, PythonTask)
        // configuration shortcut
        PythonExtension.Scope.values().each { project.extensions.extraProperties.set(it.name(), it) }

        createTasks(project, extension, envService)
    }

    private Provider<EnvService> initService(Project project) {
        // service used to shutdown docker properly and hold actual python path link
        Provider<EnvService> envService = project.gradle.sharedServices.registerIfAbsent(
                'pythonEnvironmentService', EnvService, spec -> {
            // root extension used
            PythonExtension rootExt = findRootExtension(project)

            EnvService.Params params = spec.parameters as EnvService.Params
            // only root project value counted for print stats activation
            params.printStats.set(project.provider { rootExt.printStats })
            params.debug.set(project.provider { rootExt.debug })
        })

        // it is not required, but used to prevent KILLING service too early under configuration cache
        eventsListenerRegistry.onTaskCompletion(envService)

        // IMPORTANT: do not try to obtain service here (e.g. to init it) because it would cause eager parameters
        // resolution!!!

        return envService
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    private void createTasks(Project project, PythonExtension extension, Provider<EnvService> envService) {
        // validate installed python
        TaskProvider<CheckPythonTask> checkTask = project.tasks.register('checkPython', CheckPythonTask) {
            it.description = 'Validate python environment'
        }

        // default pip install task
        TaskProvider<PipInstallTask> installTask = project.tasks.register('pipInstall', PipInstallTask) {
            it.description = 'Install pip modules'
        }

        project.tasks.register('pipUpdates', PipUpdatesTask) {
            it.description = 'Check if new versions available for declared pip modules'
        }

        project.tasks.register('pipList', PipListTask) {
            it.description = 'Show all installed modules'
        }

        project.tasks.register('cleanPython', Delete) {
            it.with {
                group = 'python'
                description = 'Removes existing python environment (virtualenv)'
                delete extension.envPath
                onlyIf { project.file(extension.envPath).exists() }
            }
        }

        configureDefaults(project, extension, checkTask, installTask, envService)
    }

    @SuppressWarnings(['MethodSize', 'AbcMetric'])
    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureDefaults(Project project,
                                   PythonExtension extension,
                                   TaskProvider<CheckPythonTask> checkTask,
                                   TaskProvider<PipInstallTask> installTask,
                                   Provider<EnvService> envService) {
        project.tasks.withType(BasePythonTask).configureEach { task ->
            task.envService.set(envService)
            task.usesService(envService)

            Environment gradleEnv = GradleEnvironment.create(project, task.name, envService,
                    project.provider { findRootExtension(project).debug })
            task.gradleEnv.set(gradleEnv)
            doLast {
                gradleEnv.printCacheState()
            }

            // setting default value from extension to all tasks, but tasks would actually check
            // service for actual pythonPath before python instance creation
            // Only checkPython task will always use this default to initialize service value
            task.pythonPath.convention(extension.pythonPath)
            task.useCustomPython.convention(false)
            task.pythonBinary.convention(extension.pythonBinary)
            task.validateSystemBinary.convention(extension.validateSystemBinary)
            task.pythonArgs.convention([])
            // important to copy map because each task must have independent instance
            // use set instead of convention to preserve global environment variables when custom vars added to task
            task.environment.set(extension.environment ? new HashMap<>(extension.environment) : [:])
            task.logLevel.convention(LogLevel.LIFECYCLE)

            // can't be implemented with convention mapping, only with properties
            configureDockerInTask(project, extension.docker, task)

            // all python tasks must be executed after check task to use correct environment (switch to virtualenv)
            if (task.taskIdentity.type != CheckPythonTask) {
                task.dependsOn checkTask
            }
        }

        project.tasks.withType(PythonTask).configureEach { task ->
            task.createWorkDir.convention(true)
            task.outputPrefix.convention('\t')
            task.extraArgs.convention([])
            // by default all python tasks must be executed after dependencies init
            task.dependsOn installTask
        }

        // apply defaults for pip tasks
        project.tasks.withType(BasePipTask).configureEach { task ->
            task.modules.convention(extension.modules)
            // in case of virtualenv checkPython will manually disable it
            task.userScope.convention(extension.scope != PythonExtension.Scope.GLOBAL)
            task.useCache.convention(extension.usePipCache)
            task.breakSystemPackages.convention(extension.breakSystemPackages)
            task.trustedHosts.convention(extension.trustedHosts)
            task.extraIndexUrls.convention(extension.extraIndexUrls)
            task.requirements.convention(RequirementsReader.find(task.gradleEnv.get(), extension.requirements))
            task.strictRequirements.convention(extension.requirements.strict)
        }

        // apply defaults for all pip install tasks (custom pip installs may be used)
        project.tasks.withType(PipInstallTask).configureEach { task ->
            task.showInstalledVersions.convention(extension.showInstalledVersions)
            task.alwaysInstallModules.convention(extension.alwaysInstallModules)
            task.options.convention([])
            task.envPath.convention(extension.envPath)
        }

        project.tasks.withType(PipListTask).configureEach { task ->
            task.all.convention(false)
        }

        project.tasks.withType(PipUpdatesTask).configureEach { task ->
            task.all.convention(false)
        }

        project.tasks.withType(CheckPythonTask).configureEach { task ->
            task.scope.convention(extension.scope)
            task.envPath.convention(extension.envPath)
            task.minPythonVersion.convention(extension.minPythonVersion)
            task.minPipVersion.convention(extension.minPipVersion)
            task.installVirtualenv.convention(extension.installVirtualenv)
            task.virtualenvVersion.convention(extension.virtualenvVersion)
            task.minVirtualenvVersion.convention(extension.minVirtualenvVersion)
            task.envCopy.convention(extension.envCopy)
        }
    }
}
