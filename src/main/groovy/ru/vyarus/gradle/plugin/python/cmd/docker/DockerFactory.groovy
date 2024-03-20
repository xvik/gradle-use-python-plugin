package ru.vyarus.gradle.plugin.python.cmd.docker

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.cmd.env.Environment

/**
 * Global docker containers manager. All python tasks, requiring the same container (by full image name) would use
 * the same instance in order to synchronize calls. The same applies for multi-module projects.
 * <p>
 * Containers re-use is important not only for synchronization, but to speed-up execution, avoiding re-starting
 * container for each python call.
 * <p>
 * Container might be restarted if target python command requires different environment, work dir or specific
 * docker configuration (but, of course, it's better to use the same docker configuration, declared in extension).
 * <p>
 * If different tasks would require different docker images - different containers would be started and they may
 * work concurrently (no synchronization required).
 *
 * @author Vyacheslav Rusakov
 * @since 23.09.2022
 */
@SuppressWarnings('SynchronizedMethod')
@CompileStatic
class DockerFactory {

    private static final Map<String, ContainerManager> CONTAINERS = [:]

    /**
     * Gets existing or creates new docker container manager. It is assumed that all tasks requiring the same container
     * (by image name) would share the same instance. This allows synchronization of running commands inside
     * the same container (so in multi-module projects or with parallel execution one container would always
     * execute only one python command).
     * <p>
     * Note that exclusive tasks always spawn new container.
     *
     * @param config docker configuration (only image name is required)
     * @param project project instance
     * @return container manager instance (most likely, already started)
     */
    static synchronized ContainerManager getContainer(DockerConfig config, Environment environment) {
        if (config == null) {
            return null
        }
        String key = config.image
        if (!CONTAINERS.containsKey(key)) {
            CONTAINERS.put(key, new ContainerManager(config.image, config.windows, environment))
        }
        return CONTAINERS.get(key)
    }

    /**
     * Shuts down started containers. Called at the end of the build.
     */
    static synchronized void shutdownAll() {
        CONTAINERS.values().each { it.stop() }
        CONTAINERS.clear()
    }

    /**
     * @return active containers count (not stopped)
     */
    static synchronized int getActiveContainersCount() {
        return CONTAINERS.values().stream().filter { !it.started }.count()
    }
}
