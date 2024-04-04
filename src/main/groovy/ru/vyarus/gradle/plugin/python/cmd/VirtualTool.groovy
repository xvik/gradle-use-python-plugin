package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import org.gradle.api.logging.LogLevel
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerConfig
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.util.CliUtils

import java.nio.file.Paths

/**
 * Base class for environment virtualization tools (venv, virtualenv).
 *
 * @author Vyacheslav Rusakov
 * @since 19.09.2023
 * @param <T>    actual tool type
 */
@CompileStatic
abstract class VirtualTool<T extends VirtualTool> {

    protected final Environment env
    protected final Python python
    final String path
    final File location

    protected VirtualTool(Environment environment, String pythonPath, String binary, String path) {
        this.env = environment
        this.python = new Python(environment, pythonPath, binary).logLevel(LogLevel.LIFECYCLE)
        this.path = path
        if (!path) {
            throw new IllegalArgumentException('Virtual environment path not set')
        }
        this.location = environment.file(path)
        environment.debug("${getClass().simpleName} environment init for path '${path}' (python path: '${pythonPath}')")
    }

    /**
     * System binary search is performed only for global python (when pythonPath is not specified). Enabled by default.
     *
     * @param validate true to search python binary in system path and fail if not found
     * @return cli instance for chained calls
     */
    T validateSystemBinary(boolean validate) {
        this.python.validateSystemBinary(validate)
        return self()
    }

    /**
     * Enable docker support: all python commands would be executed under docker container.
     *
     * @param docker docker configuration (may be null)
     * @return cli instance for chained calls
     */
    T withDocker(DockerConfig docker) {
        this.python.withDocker(docker)
        return self()
    }

    /**
     * Shortcut for {@link Python#workDir(java.lang.String)}.
     *
     * @param workDir python working directory
     * @return virtualenv instance for chained calls
     */
    T workDir(String workDir) {
        python.workDir(workDir)
        return self()
    }

    /**
     * Shortcut for {@link Python#environment(java.util.Map)}.
     *
     * @param env environment map
     * @return pip instance for chained calls
     */
    T environment(Map<String, Object> env) {
        python.environment(env)
        return self()
    }

    /**
     * Perform pre-initialization and, if required, validate global python binary correctness. Calling this method is
     * NOT REQUIRED: initialization will be performed automatically before first execution. But it might be called
     * in order to throw possible initialization error before some other logic (related to exception handling).
     *
     * @return virtualenv instance for chained calls
     */
    T validate() {
        python.validate()
        return self()
    }

    /**
     * May be used to apply additional virtualenv ({@link Python#extraArgs(java.lang.Object)}) or python
     * ({@link Python#pythonArgs(java.lang.Object)}) arguments.
     *
     * @return python cli instance used to execute commands
     */
    Python getPython() {
        return python
    }

    /**
     * @return true if virtualenv exists
     */
    boolean exists() {
        return location.exists() && location.list().size() > 0
    }

    /**
     * @return python path to use for environment
     */
    String getPythonPath() {
        return python.getOrCompute("env.python.path:$env.projectPath") {
            String res = CliUtils.pythonBinPath(location.absolutePath, python.windows)
            return Paths.get(path).absolute ? res
                    // use shorter relative path
                    : env.relativePath(res)
        }
    }

    /**
     * Create virtual environment. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy (true)}.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    abstract void create(boolean copy)

    private T self() {
        return (T) this
    }
}
