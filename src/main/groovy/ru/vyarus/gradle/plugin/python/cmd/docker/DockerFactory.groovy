package ru.vyarus.gradle.plugin.python.cmd.docker

import groovy.transform.CompileStatic
import org.gradle.api.Project

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

    // same docker instance used for all tasks and subprojects
    // might be used directly to start/stop long-lived containers (span multiple tasks)
    static synchronized ContainerManager getContainer(DockerConfig config, Project project) {
        if (config == null) {
            return null
        }
        String key = config.image
        if (!CONTAINERS.containsKey(key)) {
            CONTAINERS.put(key, new ContainerManager(config.image, config.windows, project))
        }
        return CONTAINERS.get(key)
    }

    // not required as testcontainers would close them automatically, but added for tests in order to not
    // re-use the same containers between tests (technically, it is ok to re-use containers, but just in case)
    static synchronized void shutdownAll() {
        CONTAINERS.values().each { it.stop() }
        CONTAINERS.clear()
    }
}
