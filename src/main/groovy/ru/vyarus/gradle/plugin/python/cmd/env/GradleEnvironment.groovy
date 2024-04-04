package ru.vyarus.gradle.plugin.python.cmd.env

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import ru.vyarus.gradle.plugin.python.service.EnvService
import ru.vyarus.gradle.plugin.python.service.stat.PythonStat
import ru.vyarus.gradle.plugin.python.service.value.CacheValueSource
import ru.vyarus.gradle.plugin.python.service.value.StatsValueSource

import javax.inject.Inject
import java.util.function.Supplier

/**
 * Configuration cache compatible implementation (substitutes gradle {@link org.gradle.api.Project} object usage).
 * <p>
 * Environment instance is created per python task, but all instances share same caches and stats (even under
 * configuration cache).
 * <p>
 * Configuration cache caches entire object (so it is not created when configuration cache is enabled), but
 * caches and stats are always passed resolves from (singleton) service.
 *
 * @author Vyacheslav Rusakov
 * @since 15.03.2024
 */
@CompileStatic
@SuppressWarnings(['Println', 'DuplicateStringLiteral'])
abstract class GradleEnvironment implements Environment {

    private final Logger logger
    private final File projectDir
    private final File rootDir
    private final String rootName
    private final String projectPath
    private final String taskName

    // same cache for all projects
    private final Provider<Map<String, Object>> cacheGlobal
    // same cache for all instances within one project
    private final Provider<Map<String, Object>> cacheProject
    // same list for all projects
    private final Provider<List<PythonStat>> stats
    private final Provider<Boolean> debug

    @SuppressWarnings('SynchronizedMethod')
    static synchronized Environment create(Project project, String taskName,
                                           Provider<EnvService> service, Provider<Boolean> debug) {
        // NOTE: different instance created for each task, but cache and stat instances would be the same!
        project.objects.newInstance(GradleEnvironment,
                project.logger, project.projectDir, project.rootDir, project.rootProject.name, project.path, taskName,
                // gradle can't cache it - always the same instance!
                project.providers.of(CacheValueSource) {
                    it.parameters.service.set(service)
                    it.parameters.project.set(project.rootProject.path)
                },
                project.providers.of(CacheValueSource) {
                    it.parameters.service.set(service)
                    it.parameters.project.set(project.path)
                },
                project.providers.of(StatsValueSource) {
                    it.parameters.service.set(service)
                },
                debug)
    }

    @Inject
    @SuppressWarnings(['ParameterCount', 'AbstractClassWithPublicConstructor'])
    GradleEnvironment(Logger logger,
                      File projectDir,
                      File rootDir,
                      String rootName,
                      String projectPath,
                      String taskName,
                      Provider<Map<String, Object>> globalCache,
                      Provider<Map<String, Object>> projectCache,
                      Provider<List<PythonStat>> stats,
                      Provider<Boolean> debug) {
        this.logger = logger
        this.projectDir = projectDir
        this.rootDir = rootDir
        this.rootName = rootName
        this.projectPath = projectPath
        this.taskName = taskName
        // note for root project it would be the same maps
        this.cacheGlobal = globalCache
        this.cacheProject = projectCache
        this.stats = stats
        this.debug = debug
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
    String relativeRootPath(String path) {
        String res = file(path).absolutePath
        String root = rootDir.absolutePath
        if (res.startsWith(root)) {
            res = res.replace(root, '')
            if (res.startsWith('/') || res.startsWith('\\')) {
                res = res.substring(1)
            }
        }
        return res
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
        return getOrCompute(false, cacheProject.get(), key, value)
    }

    @Override
    public <T> T globalCache(String key, Supplier<T> value) {
        return getOrCompute(true, cacheGlobal.get(), key, value)
    }

    @Override
    void updateProjectCache(String key, Object value) {
        Map<String, Object> cache = cacheProject.get()
        if (debug.get()) {
            // instance important for configuration cache mode where different objects could appear (but shouldn't
            // because ValueSource objects used)
            println "[CACHE$taskPath] Project cache update $key=$value (instance: " +
                    "${System.identityHashCode(cache)})"
        }
        cache.put(key, value)
    }

    @Override
    void updateGlobalCache(String key, Object value) {
        Map<String, Object> cache = cacheGlobal.get()
        if (debug.get()) {
            println "[CACHE$taskPath] Global cache update $key=$value (instance: " +
                    "${System.identityHashCode(cache)})"
        }
        cache.put(key, value)
    }

    @Override
    @SuppressWarnings('ConfusingMethodName')
    void debug(String msg) {
        if (debug.get()) {
            println "[DEBUG$taskPath] $msg"
        }
    }

    @Override
    void stat(String containerName, String cmd, String workDir, boolean globalPython, long start, boolean success) {
        List<PythonStat> statList = stats.get()
        synchronized (statList) {
            statList.add(new PythonStat(
                    containerName: containerName,
                    projectPath: projectPath,
                    taskName: taskName,
                    cmd: cmd,
                    workDir: relativeRootPath((workDir != null ? file(workDir) : projectDir).absolutePath),
                    start: start,
                    success: success,
                    duration: System.currentTimeMillis() - start
            ))
            if (debug.get()) {
                println "[STATS$taskPath] Stat registered: stats instance " +
                        "${System.identityHashCode(statList)}, count ${statList.size()}\n\tfor: $cmd"
            }
        }
    }

    @Override
    void printCacheState() {
        if (debug.get()) {
            StringBuilder res = new StringBuilder('\n--------------------------------------------------- state after '
                    + "${"$projectPath:$taskName".replaceAll('::', ':')} \n")
            if (projectPath == ':') {
                res.append('\n\tGLOBAL CACHE is the same as project cache for root project\n')
            } else {
                Map<String, Object> cache = cacheGlobal.get()
                synchronized (cache) {
                    res.append("\tGLOBAL CACHE (instance ${System.identityHashCode(cache)}) [${cache.size()}]\n")
                    new TreeSet<>(cache.keySet()).each { res.append("\t\t$it = ${cache.get(it)}\n") }
                }
            }

            Map<String, Object> cache = cacheProject.get()
            synchronized (cache) {
                res.append("\n\tPROJECT CACHE (instance ${System.identityHashCode(cache)}) [${cache.size()}]\n")
                new TreeSet<>(cache.keySet()).each { res.append("\t\t$it = ${cache.get(it)}\n") }
            }

            res.append('-------------------------------------------------------------------------\n')
            println res.toString()
        }
    }

    @Inject
    protected abstract ExecOperations getExec()

    @Inject
    protected abstract FileOperations getFs()

    private <T> T getOrCompute(boolean global, Map<String, Object> cache, String key, Supplier<T> value) {
        synchronized (cache) {
            // computeIfAbsent not used here because actions MAY also call cacheable functions, which is not allowed
            T res = cache.get(key) as T
            if (debug.get() && res != null) {
                println("[CACHE$taskPath] Use cached value: $key = $value")
            }
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

    private String getTaskPath() {
        return "$projectPath:$taskName".replaceAll('::', ':')
    }
}
