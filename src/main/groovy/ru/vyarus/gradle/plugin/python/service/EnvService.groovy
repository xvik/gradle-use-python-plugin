package ru.vyarus.gradle.plugin.python.service

import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerFactory

import java.util.concurrent.ConcurrentHashMap

/**
 * Service holds actual link to used python path. Actual path is resolved after virtual environment creation or
 * detection.
 * <p>
 * Also, configuration cache compatible way to listen for build finish (instead of project.gradle.buildFinished).
 *
 * @author Vyacheslav Rusakov
 * @since 14.03.2024
 */
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
@CompileStatic
abstract class EnvService implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    private final Logger logger = Logging.getLogger(EnvService)

    private final Object sync = new Object()
    // project path -- python path
    private final Map<String, String> pythonPaths = new ConcurrentHashMap<>()
    // project path -- default python path provider
    private final Map<String, Provider<String>> defaultProviders = new ConcurrentHashMap<>()

    void defaultProvider(String projectPath, Provider<String> provider) {
        defaultProviders.put(projectPath, provider)
    }

    String getPythonPath(String projectPath) {
        synchronized (sync) {
            String path = pythonPaths.get(projectPath)
            // lazy init
            if (path == null) {
                Provider<String> init = defaultProviders.remove(projectPath)
                if (init != null) {
                    path = init.get()
                    if (path != null) {
                        pythonPaths.put(projectPath, path)
                    }
                }
            }
            return path
        }
    }

    void setPythonPath(String projectPath, String pythonPath) {
        synchronized (sync) {
            logger.info('Python path for {} changed to {}', projectPath, pythonPath)
            this.pythonPaths.put(projectPath, pythonPath)
        }
    }

    @Override
    void close() throws Exception {
        logger.info('Shutdown docker containers ({} active)', DockerFactory.activeContainersCount)
        // close started docker containers at the end (mainly for tests, because docker instances are
        // project-specific and there would be problem in gradle tests always started in new dir)
        DockerFactory.shutdownAll()
    }
}
