package ru.vyarus.gradle.plugin.python.service

import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerFactory
import ru.vyarus.gradle.plugin.python.service.stat.PythonStat
import ru.vyarus.gradle.plugin.python.service.stat.StatsPrinter

import java.util.concurrent.ConcurrentHashMap

/**
 * Service manage all required state for plugin: resolved python paths, cache and stats. One service instance used
 * for all projects. This service is essential for configuration cache compatibility.
 * <p>
 * All python tasks have pythonPath property, initialized with extension default value. But checkPython task
 * could change this default (due to switching to virtualenv) and so this service holds all actual paths
 * (for all projects). Python tasks read path value directly from service instead of task property in order to use
 * correct value (could be overridden in exact task).
 * <p>
 * Also, service is the only way to collect python execution stats in one place and manage cache instances
 * (gradle properties are not available under configuration cache). Note that gradle could not "cache" python
 * executions cache state in any case because configuration cache records state before actual execution (where
 * all python executions occur).
 *
 * @author Vyacheslav Rusakov
 * @since 14.03.2024
 */
@SuppressWarnings(['AbstractClassWithoutAbstractMethod', 'Println'])
@CompileStatic
abstract class EnvService implements BuildService<Params>, OperationCompletionListener, AutoCloseable {

    private final Logger logger = Logging.getLogger(EnvService)

    private final Object sync = new Object()

    // project path -- python path
    private final Map<String, String> pythonPaths = new ConcurrentHashMap<>()

    // caches managed in service to proper support for configuration cache (be able to use same cache instances)
    private final Map<String, Map<String, Object>> caches = new ConcurrentHashMap<>()
    // python execution statistics
    private final List<PythonStat> stats = []

    /**
     * Called by {@link ru.vyarus.gradle.plugin.python.task.CheckPythonTask} to set actual path (counting
     * virtual environment). Would be called one or two times for each project: first default value and second when
     * switching to environment
     *
     * @param projectPath project path
     * @param pythonPath python path to use
     */
    void setPythonPath(String projectPath, String pythonPath) {
        synchronized (sync) {
            if (pythonPath != null) {
                this.pythonPaths.put(projectPath, pythonPath)
            } else {
                this.pythonPaths.remove(projectPath)
            }
        }
        if (parameters.debug.get()) {
            println("[DEBUG] set python path for '$projectPath' to $pythonPath")
        }
        logger.info('Python path for {} changed to {}', projectPath, pythonPath)
    }

    /**
     * {@link ru.vyarus.gradle.plugin.python.task.CheckPythonTask} always runs before other python tasks in order to
     * select correct python environment (create virtual environment if required). All other tasks have to use
     * service value directly because only it would contain actual path (tasks pythonPath properties would only
     * contain extension default).
     *
     * @param projectPath project path
     * @return actual python path to use in all tasks (which honor virtual environments)
     */
    String getPythonPath(String projectPath) {
        synchronized (sync) {
            return pythonPaths.get(projectPath)
        }
    }

    /**
     * NOTE: Don't use directly for configuration-cacheable objects (otherwise there would be different maps in all
     * places when gradle is working from configuration cache).
     * Use {@link ru.vyarus.gradle.plugin.python.service.value.CacheValueSource}.
     *
     * @param project project path
     * @return project-wide cache instance
     */
    Map<String, Object> getCache(String project) {
        synchronized (sync) {
            Map<String, Object> res = caches.get(project)
            if (res == null) {
                res = new ConcurrentHashMap<>()
                caches.put(project, res)
            }
            return res
        }
    }

    /**
     * NOTE: don't use directly! Instead use {@link ru.vyarus.gradle.plugin.python.service.value.StatsValueSource}.
     *
     * @return stats container which must be used by all python instance
     */
    List<PythonStat> getStats() {
        return stats
    }

    @Override
    @SuppressWarnings('EmptyMethodInAbstractClass')
    void onFinish(FinishEvent finishEvent) {
        // not used - just a way to prevent killing service too early
    }

    @Override
    void close() throws Exception {
        if (parameters.debug.get()) {
            println "[DEBUG] Shutdown python docker containers ${DockerFactory.activeContainersCount}"
        }
        // close started docker containers at the end (mainly for tests, because docker instances are
        // project-specific and there would be problem in gradle tests always started in new dir)
        DockerFactory.shutdownAll()
        if (parameters.printStats.get()) {
            String stats = StatsPrinter.print(stats)
            if (stats != null && !stats.empty) {
                println stats
            }
        }
    }

    interface Params extends BuildServiceParameters {
        Property<Boolean> getPrintStats()

        Property<Boolean> getDebug()
    }

}
