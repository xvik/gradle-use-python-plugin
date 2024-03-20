package ru.vyarus.gradle.plugin.python.cmd.env

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

/**
 * Configuration cache compatible implementation (substitutes gradle {@link org.gradle.api.Project} object usage).
 * <p>
 * Caches stored in gradle extended properties so all environment entities could share the same cache maps.
 * For root project global and project cache is the same map.
 *
 * @author Vyacheslav Rusakov
 * @since 15.03.2024
 */
@CompileStatic
abstract class GradleEnvironment implements Environment {

    private static final String CACHE_KEY = 'plugin.python.project.cache'

    private final Logger logger
    private final File projectDir
    private final File rootDir
    private final String rootName
    private final String projectPath
    private final Map<String, Object> cacheGlobal
    private final Map<String, Object> cacheProject

    @SuppressWarnings('SynchronizedMethod')
    static synchronized Environment create(Project project) {
        // NOTE: different instance created for each task, but cache instance would be the same!
        project.objects.newInstance(GradleEnvironment,
                project.logger, project.projectDir, project.rootDir, project.rootProject.name, project.path,
                lookupCache(project.rootProject), lookupCache(project))
    }

    @Inject
    @SuppressWarnings(['ParameterCount', 'AbstractClassWithPublicConstructor'])
    GradleEnvironment(Logger logger,
                                File projectDir,
                                File rootDir,
                                String rootName,
                                String projectPath,
                                Map<String, Object> globalCache,
                                Map<String, Object> projectCache) {
        this.logger = logger
        this.projectDir = projectDir
        this.rootDir = rootDir
        this.rootName = rootName
        this.projectPath = projectPath
        // note for root project it would be the same maps
        this.cacheGlobal = globalCache
        this.cacheProject = projectCache
    }

    Logger getLogger() {
        return logger
    }

    @Override
    String getRootName() {
        return rootName
    }

    @Override
    String getProjectPath() {
        return projectPath
    }

    @Override
    File getRootDir() {
        return rootDir
    }

    @Override
    File getProjectDir() {
        return projectDir
    }

    @Override
    File file(String path) {
        return fs.file(path)
    }

    @Override
    String relativePath(String path) {
        return fs.relativePath(path)
    }

    @Override
    String relativePath(File file) {
        return fs.relativePath(file)
    }

    @Override
    int exec(String[] cmd, OutputStream out, OutputStream err, String workDir, Map<String, Object> envVars) {
        ExecResult res = exec.exec new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                spec.commandLine cmd
                spec.standardOutput = out
                spec.errorOutput = err
                spec.ignoreExitValue = true
                if (workDir) {
                    spec.workingDir = workDir
                }
                if (envVars) {
                    spec.environment(envVars)
                }
            }
        }
        return res.exitValue
    }

    @Override
    public <T> T projectCache(String key, Supplier<T> value) {
        return getOrCompute(false, cacheProject, key, value)
    }

    @Override
    public <T> T globalCache(String key, Supplier<T> value) {
        return getOrCompute(true, cacheGlobal, key, value)
    }

    @Override
    void updateProjectCache(String key, Object value) {
        logger.info("[$projectPath] updated cache value: $key = $value")
        cacheProject.put(key, value)
    }

    @Override
    void updateGlobalCache(String key, Object value) {
        logger.info("[$projectPath] updated global cache value: $key = $value")
        cacheGlobal.put(key, value)
    }

    @Inject
    protected abstract ExecOperations getExec()

    @Inject
    protected abstract FileOperations getFs()

    private static Map<String, Object> lookupCache(Project project) {
        Map<String, Object> cache = project.findProperty(CACHE_KEY) as Map<String, Object>
        if (cache == null) {
            cache = new ConcurrentHashMap<>()
            project.extensions.extraProperties.set(CACHE_KEY, cache)
        }
        return cache
    }

    private <T> T getOrCompute(boolean global, Map<String, Object> cache, String key, Supplier<T> value) {
        synchronized (cache) {
            // computeIfAbsent not used here because actions MAY also call cacheable functions, which is not allowed
            T res = cache.get(key) as T
            if (res == null && value != null) {
                res = value.get()
                if (global) {
                    updateGlobalCache(key, res)
                } else {
                    updateProjectCache(key, res)
                }
            }
            return res
        }
    }
}
