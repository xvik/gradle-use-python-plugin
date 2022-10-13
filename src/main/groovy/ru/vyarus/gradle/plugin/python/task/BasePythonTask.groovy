package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.util.ClosureBackedAction
import ru.vyarus.gradle.plugin.python.cmd.Python
import ru.vyarus.gradle.plugin.python.cmd.docker.ContainerManager
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerConfig
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerFactory
import ru.vyarus.gradle.plugin.python.util.OutputLogger

/**
 * Base task for all python tasks.
 *
 * @author Vyacheslav Rusakov
 * @since 01.12.2017
 */
@CompileStatic
class BasePythonTask extends ConventionTask {

    /**
     * Path to directory with python executable. Not required if python installed globally.
     * Automatically set from {@link ru.vyarus.gradle.plugin.python.PythonExtension#pythonPath}, but could
     * be overridden manually.
     */
    @Input
    @Optional
    String pythonPath

    /**
     * Python binary name. When empty: use python3 or python for linux and python for windows.
     * Automatically set from {@link ru.vyarus.gradle.plugin.python.PythonExtension#pythonBinary}, but could
     * be overridden manually.
     */
    @Input
    @Optional
    String pythonBinary

    /**
     * Manual search for global python binary (declared in {@link #pythonBinary} in system paths (PATH variable)).
     * Used to quickly reveal problems with process PATH (not the same as in user shell).
     * Validation is performed only when {@link #pythonPath} is not declared (because otherwise it does not make sense).
     * Automatically set from {@link ru.vyarus.gradle.plugin.python.PythonExtension#validateSystemBinary}, but could
     * be overridden manually.
     */
    @Input
    boolean validateSystemBinary

    /**
     * Python arguments applied to all executed commands. Arguments applied before called command
     * (and so option may be useful for cases impossible with {@link PythonTask#extraArgs}, applied after command).
     * For example, it could be used for -I or -S flags (be aware that -S can cause side effects, especially
     * inside virtual environments).
     */
    @Input
    @Optional
    List<String> pythonArgs = []

    /**
     * Environment variables for executed python process (variables specified in gradle's
     * {@link org.gradle.process.ExecSpec#environment(java.util.Map)} during python process execution).
     */
    @Input
    @Optional
    // note: environment may be initialized with global variables from extension (by mapping)
    // but not if task property configured directly (task.environment = ...)
    Map<String, Object> environment

    /**
     * Working directory. Not required, but could be useful for some modules (e.g. generators).
     */
    @Input
    @Optional
    String workDir

    /**
     * Python logs output level. By default it's {@link org.gradle.api.logging.LogLevel@LIFECYCLE}
     * (visible with '-i' gradle flag).
     */
    @Input
    @Optional
    LogLevel logLevel = LogLevel.LIFECYCLE

    @Nested
    DockerEnv docker = project.objects.newInstance(DockerEnv)

    protected BasePythonTask() {
        group = 'python'
    }

    /**
     * Add python arguments, applied before command.
     *
     * @param args arguments
     */
    @SuppressWarnings('ConfusingMethodName')
    void pythonArgs(String... args) {
        if (args) {
            getPythonArgs().addAll(args)
        }
    }

    /**
     * Add environment variable for python process (will override previously set value).
     *
     * @param name variable name
     * @param value variable value
     */
    @SuppressWarnings('ConfusingMethodName')
    void environment(String name, Object value) {
        // do like this to unify variables logic (including potential global vars from extension)
        environment([(name): value])
    }

    /**
     * Add environment variables for python process (will override already set values, but not replace context
     * map completely). May be called multiple times: all variables would be aggregated.
     *
     * @param vars (may be null)
     */
    @SuppressWarnings('ConfusingMethodName')
    void environment(Map<String, Object> vars) {
        if (vars) {
            Map<String, Object> envs = getEnvironment() ?: [:]
            envs.putAll(vars)
            // do like this to workaround convention mapping mechanism which will treat empty map as incorrect value
            setEnvironment(envs)
        }
    }

    /**
     * Configure docker container for python execution inside docker.
     *
     * @param closure configuration closure
     * @return configured docker sub-configuration object
     */
    @SuppressWarnings('ConfusingMethodName')
    DockerEnv docker(@DelegatesTo(value = DockerEnv, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        docker(new ClosureBackedAction<DockerEnv>(closure))
    }

    /**
     * Configure docker container for python execution inside docker.
     *
     * @param action configuration action
     * @return configured  docker sub-configuration object
     */
    @SuppressWarnings('ConfusingMethodName')
    DockerEnv docker(Action<? super DockerEnv> action) {
        action.execute(docker)
        return docker
    }

    @Internal
    protected Python getPython() {
        buildPython()
    }

    /**
     * Takes into account docker configuration (container os).
     *
     * @return true if target os is windows, false otherwise
     */
    @Internal
    protected boolean isWindows() {
        return python.windows
    }

    /**
     * Shortcut for custom tasks (simplify usage and avoid silly errors with properties usage).
     *
     * @return true if docker environment enabled, false otherwise
     */
    @Internal
    protected boolean isDockerUsed() {
        return docker.use.get()
    }

    /**
     * Execute command inside docker container. Output would be printed to console.
     * <p>
     * IMPORTANT: exception would not be thrown if command execution fails!
     *
     * @param cmd command array to execute (paths to project file would be re-written to docker paths)
     * @return command exit code
     * @throws GradleException when docker is not configured for task
     */
    protected int dockerExec(String... cmd) {
        logger.lifecycle('[docker] executing command: {}', cmd.join(' '))
        // start command printing all output messages
        return new OutputLogger(logger, LogLevel.LIFECYCLE, '\t')
                .withStream { return dockerExec(it, cmd) }
    }

    /**
     * Execute command inside docker container. Command output would be only written into provided stream, but
     * after process finishes.
     * <p>
     * IMPORTANT: exception would not be thrown if command execution fails!
     *
     * @param out output stream (use {@link ByteArrayOutputStream} to consume output)
     * @param cmd command array to execute (paths to project file would be re-written to docker paths)
     * @return command exit code
     * @throws GradleException when docker is not configured for task
     */
    protected int dockerExec(OutputStream out, String... cmd) {
        if (!getDocker().use) {
            throw new GradleException('Docker command can\'t be executed: docker not enabled')
        }
        // it would be pre-started container (used in checkPython)
        ContainerManager manager = DockerFactory.getContainer(getDocker().toConfig(), project)
        // restart container if task parameters differ
        manager.restartIfRequired(getDocker().toConfig(), getWorkDir(), getEnvironment())
        // rewrite paths from host to docker fs
        manager.convertCommand(cmd)
        // start command printing all output messages
        return manager.exec(cmd, out)
    }

    @Memoized
    private Python buildPython() {
        new Python(project, getPythonPath(), getPythonBinary())
                .logLevel(getLogLevel())
                .workDir(getWorkDir())
                .pythonArgs(getPythonArgs())
                .environment(getEnvironment())
                .validateSystemBinary(getValidateSystemBinary())
                .withDocker(getDocker().toConfig())
                .validate()
    }

    /**
     * Task-specific configuration for docker environment. By default, copies configuration from extension
     * {@link ru.vyarus.gradle.plugin.python.PythonExtension#docker}, but configuration might be changed for each task.
     * <p>
     * Docker container is re-used between python executions, but if task configuration differs from configuration
     * used for already started container (work dit, environment, docker specific configs) then container would be
     * re-started.
     * <p>
     * For a long-lived tasks use {@link DockerEnv#getExclusive()} mode.
     *
     * @see ru.vyarus.gradle.plugin.python.cmd.docker.DockerFactory
     */
    static abstract class DockerEnv {

        /**
         * @return true when docker container must be used
         */
        @Input
        abstract Property<Boolean> getUse()

        /**
         * Docker image to use. This is complete image path (potentially including repository and tag) and not just
         * image name. It is highly suggested always specifying exact tag!
         * <p>
         * Normally, docker image must align with host. Windows image would not work on linux (without additional
         * virtualization). But, on windows, it is possible to launch linux container. Anyway, in order to minimize
         * problems, host-specific images would be used by default.
         *
         * @see <a href="https://hub.docker.com/_/python">python image</a>
         */
        @Input
        abstract Property<String> getImage()

        /**
         * Type of used image. Normally, image os must be aligned with host, but variations are possible,
         * There is no automatic system detection in docker so you'll need to specify correct type to let plugin
         * correctly format commands.
         */
        @Internal
        abstract Property<Boolean> getWindows()

        /**
         * By default, same container instance is re-used between all python executions. But, this cause limitations:
         * <ul>
         *     <li>Task execution might be limited in time (due to http api used for docker control)
         *     <li>Task logs would appear only AFTER task execution
         * </ul>
         * <p>
         * For long-running processes it is better to start container exclusively (use python command as docker
         * container command, which will hold container as long as command would be executed, moreover logs would be
         * immediately visible). One example, is some dev-server which could work indefinitely (like mkdocs server,
         * used in time of writing documentation).
         */
        @Input
        abstract Property<Boolean> getExclusive()

        /**
         * Required container port mappings - port to open from container to be accessible on host.
         * Note that normally ports are not required because python code executed inside container. This could
         * make sense for long-lived process like dev.server.
         * <p>
         * Single number (2011) for mapping on the same port and colo-separated numbers (2011:3011) for mapping
         * on custom port.
         */
        @Input
        abstract SetProperty<String> getPorts()

        /**
         * Specify ports to expose from container. Value could be either integer or string. By default, port would
         * be mapped on the same port on host (no random), but if different port is required use 'port:port' string.
         *
         * @param ports ports to be mapped from container
         */
        @SuppressWarnings('UnnecessaryGetter')
        void ports(Object... ports) {
            ports.each { getPorts().add(String.valueOf(it)) }
        }

        /**
         * @return docker configuration for managed container creation or null if docker not required
         */
        @SuppressWarnings('UnnecessaryGetter')
        DockerConfig toConfig() {
            getUse().get() ? new DockerConfig(
                    image: getImage().get(),
                    windows: getWindows().get(),
                    exclusive: getExclusive().get(),
                    ports: getPorts().get() as Set<String>) : null
        }
    }
}
