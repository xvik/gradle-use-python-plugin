package ru.vyarus.gradle.plugin.python.cmd.docker

import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.Container
import org.testcontainers.containers.output.OutputFrame
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.util.DurationFormatter

import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Encapsulates work with single docker container. Normally, container is started before first python call and
 * stopped only after gradle execution. Container re-use slightly speeds-up execution (moreover, it is required
 * for virtualenv creation as container is stateless and can't preserver virtualenv installation on restart).
 * <p>
 * All python commands targeting the same container would be executed in one managed instance, even for multi-module
 * project. Moreover, such executions would be synchronized to avoid side effects like ports clashing (random ports
 * can't be used).
 * <p>
 * Container will re-start automatically if some task has different requirements: docker configuration (except image),
 * environment variables or working directory. This was done to avoid confusing behaviours when changed configuration
 * for task not applied to started container.
 * <p>
 * Limitations of running python commands on already started container:
 * <ul>
 *     <li>Task execution might be limited in time (due to http api used for docker control)
 *     <li>Task logs would appear only AFTER task execution
 * </ul>
 * <p>
 * For long-running tasks, exclusive mode could be enabled on task level. For such task new container would be
 * started using python command as container command (and so container would stop working after this command).
 * This could be suitable, for example, for dev servers.
 * <p>
 * Root project directory is mapped inside docker container and all command paths are automatically re-written
 * according to mapping.
 *
 * @author Vyacheslav Rusakov
 * @since 12.09.2022
 * @see DockerFactory
 */
@SuppressWarnings(['SynchronizedMethod', 'DuplicateStringLiteral'])
@CompileStatic
class ContainerManager {

    private static final String DOCKER_PROJECT_PATH = '/usr/src/'
    private static final String DOCKER_WINDOWS_PROJECT_PATH = 'c:/projects/'
    private static final byte[] EMPTY = new byte[0]
    private static final String NL = '\n'
    private static final char WIN_SEPARATOR = '\\'
    private static final char LINUX_SEPARATOR = '/'
    private static final int STARTUP_TIMEOUT = 3

    private final String image
    private final boolean windowsImage
    private final Environment environment
    private final String projectRootPath
    private final String projectDockerPath

    private PythonContainer container
    private Config containerConfig

    ContainerManager(String image, boolean windows, Environment environment) {
        this.image = image
        this.windowsImage = windows
        this.environment = environment
        this.projectRootPath = environment.rootDir.absolutePath
        this.projectDockerPath =
                (windows ? DOCKER_WINDOWS_PROJECT_PATH : DOCKER_PROJECT_PATH) + environment.rootName
    }

    /**
     * @return true if windows docker image used, false otherwise
     */
    boolean isWindows() {
        return windowsImage
    }

    /**
     * Replaces local paths into docker paths (according to project mapping). Only for paths leading inside
     * project. This way the same command would work with local python and dockerized python (no need to correct
     * anything).
     * <p>
     * Also replace path separators to match target container OS. For example, if linux container executed on
     * windows, paths would be formatted for windows which would cause problems inside container.
     *
     * @param command command to convert paths in
     */
    void convertCommand(String[] command) {
        for (int i = 0; i < command.length; i++) {
            command[i] = toDockerPath(command[i])
        }
    }

    /**
     * Converts path to docker path, according to mapping (only for absolute paths, leading inside project).
     * <p>
     * Also replace path separators to match target container OS. For example, if linux container executed on
     * windows, paths would be formatted for windows which would cause problems inside container.
     *
     * @param path path to convert
     * @return converted path (or not changed, if conversion not require)
     * @see #toLocalPath(java.lang.String) reverse conversion
     */
    String toDockerPath(String path) {
        return canonicalPath(path.replace(projectRootPath, projectDockerPath))
    }

    /**
     * Replace path separators to match target container OS. For example, if linux container executed on
     * windows, paths would be formatted for windows which would cause problems inside container.
     *
     * @param path path
     * @return path with updated separators according to target os
     */
    String canonicalPath(String path) {
        String res = path
        char from = Os.isFamily(Os.FAMILY_WINDOWS) ? WIN_SEPARATOR : LINUX_SEPARATOR
        char to = windows ? WIN_SEPARATOR : LINUX_SEPARATOR
        if (from != to) {
            // there probably would be edge cases with false replacements.. practice will show
            res = res.replace(from, to)
        }
        return res
    }

    /**
     * Converts docker path into local path (only for absolute paths, leading inside project). If docker path does
     * not lead to project directory returns null to indicate impossible conversion.
     *
     * @param path path to convert
     * @return converted path (or same path if its already a local project path) or null
     */
    String toLocalPath(String path) {
        if (path.startsWith(projectRootPath)) {
            // already local path
            return path
        } else if (path.startsWith(projectDockerPath)) {
            return path.replace(projectDockerPath, projectRootPath)
        }
        // returns null to indicate that path CAN'T be remapped back (is not located inside project)
        return null
    }

    /**
     * Compares started container configuration with python command requirements and restart container with
     * new configuration. Such automatic restart should solve many problems with "fine-tuning" of python tasks.
     *
     * @param config docker configuration
     * @param workDir working directory (may be null)
     * @param envVars environment variables (may be null)
     */
    synchronized void restartIfRequired(DockerConfig config, String workDir, Map<String, Object> envVars) {
        Config upd = new Config(config, workDir, envVars)
        if (container == null) {
            start(upd)
        } else {
            // compare started container configuration
            String diff = containerConfig.diff(upd)
            if (diff) {
                environment.logger.lifecycle('Restarting container due to changed {}', diff)
                stop()
                start(upd)
            }
        }
    }

    /**
     * @return true if container is started
     */
    boolean isStarted() {
        return container != null
    }

    /**
     * @return container name or null if container not started yet
     */
    String getContainerName() {
        return container?.running ? container.containerName : null
    }

    /**
     * Stops started container or do nothing if not started.
     */
    synchronized void stop() {
        if (container && container.running) {
            doStop('container', container)
            container = null
            containerConfig = null
        }
    }

    /**
     * Execute python command in pre-started container. As a limitation, command output will be available
     * only after command execution.
     * <p>
     * Execution is synchronized and so only one execution at a time in THE SAME container is possible. Different
     * containers (or exclusive tasks) could run concurrently. Synchronization is required to avoid potential side
     * effects of parallel execution, for example, with port mappings (random ports can't be used).
     *
     * @param command command to execute
     * @param out stream to write output into
     * @return process exit code
     */
    synchronized int exec(String[] command, OutputStream out) {
        long watch = System.currentTimeMillis()
        Container.ExecResult res = container.execInContainer(StandardCharsets.UTF_8, command)
        environment.logger.info('Command executed in pre-started container ({}) in {}',
                container.containerName, DurationFormatter.format(System.currentTimeMillis() - watch))
        if (res.stdout != null) {
            out.write(res.stdout.getBytes(StandardCharsets.UTF_8))
        }
        if (res.stderr != null) {
            out.write(res.stderr.getBytes(StandardCharsets.UTF_8))
        }
        return res.exitCode
    }

    /**
     * Runs python command in exclusive container (container started with command). If there is already pre-started
     * container - it would not be stopped, so other python tasks (not exclusive) could continue using it.
     * <p>
     * In contrast to in-container execution, exclusive execution streams logs immediately. This makes exclusive
     * mode ideal for long-running tasks or when immediate logs are important. For example, it might be used for a
     * dev. server.
     * <p>
     * Technically, this method does not belong to container manager, but it would remain here in order to easily
     * apply synchronization in the future (if required).
     *
     * @param command command to execute
     * @param out stream to write output into
     * @param config docker configuration
     * @param workDir working directory (inside project!) or null
     * @param env environment variables (or null)
     * @return 0 for successful execution and 1 in case of error (real error exit code unknown)
     */
    @SuppressWarnings('BusyWait')
    int execExclusive(String[] command,
                      OutputStream out,
                      DockerConfig config,
                      String workDir,
                      Map<String, Object> env) {
        PythonContainer cont = createContainer(config, workDir, env)
                .withCommand(command)
                .withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT))
//                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        // output live stream
                .withLogConsumer { OutputFrame frame -> out.write(frame.bytes ?: EMPTY) }
        try {
            doStart('exclusive container', cont)
            // In case of forever-running task, gradle would interrupt task thread when ctrl+c would be called
            // in console. After this point we will not see any logs anymore, but testcontainers should still
            // shut down started containers.
            while (cont.running && !Thread.currentThread().interrupted) {
                sleep(300)
            }
        } catch (GradleException ex) {
            environment.logger.lifecycle('Exclusive container failed to start: {}', ex.message)
            // normally it would not be visible, but it would be possible to see it with --info flag
            environment.logger.info('Container error stacktrace', ex)
        } finally {
            // if container already finished (limited time task), then no log here (no actual stop performed)
            // and for infinite task user will not see this log (stopping just in case)
            doStop('exclusive container', cont)
        }
        return cont.dockerClient.inspectContainerCmd(cont.containerId).exec().state.exitCodeLong
    }

    @SuppressWarnings('UnnecessaryGetter')
    String formatContainerInfo(DockerConfig config, String workDir, Map<String, Object> env) {
        StringBuilder res = new StringBuilder()
        String labelPattern = '\t%-15s '
        res.append(String.format(labelPattern, 'Mount'))
                .append(projectRootPath).append(':').append(projectDockerPath).append(NL)
        res.append(String.format(labelPattern, 'Work dir'))
                .append(getDockerWorkDir(workDir)).append(NL)
        if (env != null && !env.isEmpty()) {
            res.append(String.format(labelPattern, "Env (${env.size()})"))
            // only names because there might be secrets in values
                    .append(env.keySet().join(', ')).append(NL)
        }

        if (config.useHostNetwork) {
            res.append(String.format(labelPattern, 'Host network')).append(NL)
        } else if (config.ports != null && !config.ports.empty) {
            res.append(String.format(labelPattern, 'Ports')).append(config.ports.join(', ')).append(NL)
        }
        return res.toString()
    }

    @SuppressWarnings('Indentation')
    private void start(Config config) {
        if (container != null) {
            return
        }
        container = createContainer(config.docker, config.workDir, config.env)
        // infinite process to keep container running
        if (windows) {
            container.withCommand('ping', '-t', 'localhost', '>', 'NUL')
        } else {
            container.withCommand('tail', '-f', '/dev/null')
        }
        container.withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT))
                .withLogConsumer { OutputFrame frame ->
                    if (frame.bytes != null) {
                        environment.logger.lifecycle('[docker{}] {}', container.containerName, frame.utf8String)
                    }
                }
        doStart('container', container, config.docker, config.workDir, config.env)
        containerConfig = config
    }

    @SuppressWarnings('CatchException')
    private void doStart(String message, PythonContainer container,
                         DockerConfig config = null,
                         String workDir = null,
                         Map<String, Object> env = null) {
        long watch = System.currentTimeMillis()
        try {
            container.start()
        } catch (Exception ex) {
            // by default gradle shows only top error message and user need to activate --stacktrace mode to see
            // inner errors, but the most important information would be in these inner messages
            // To simplify usage throwing onw more exception with the root cause
            throw new GradleException(collectErrors(ex), ex)
        }
        String info = config != null ? formatContainerInfo(config, workDir, env) : ''
        environment.logger.lifecycle('[docker] {} \'{}\' ({}) started in {}{}',
                message, image, container.containerName, DurationFormatter.format(System.currentTimeMillis() - watch),
                NL + info)
    }

    private String collectErrors(Exception ex) {
        String res = ex.message
        String prefix = ''
        Throwable current = ex
        while (current.cause) {
            prefix += '\t'
            current = current.cause
            res += "\n$prefix ${current.message}"
        }
        return res
    }

    @SuppressWarnings('CatchException')
    private void doStop(String message, PythonContainer container) {
        if (container.running) {
            long watch = System.currentTimeMillis()
            String name = container.containerName
            try {
                container.stop()
                environment.logger.lifecycle('[docker] {} \'{}\' ({}) stopped in {}',
                        message, image, name, DurationFormatter.format(System.currentTimeMillis() - watch))
            } catch (Exception ex) {
                environment.logger.warn("Container '$name' stop failed", ex)
            }
        }
    }

    @SuppressWarnings('UnnecessaryGetter')
    private PythonContainer createContainer(DockerConfig config, String workDir, Map<String, Object> env) {
        PythonContainer container = new PythonContainer(image)
                .withFileSystemBind(projectRootPath, projectDockerPath, BindMode.READ_WRITE)
                .withWorkingDirectory(getDockerWorkDir(workDir))

        if (env != null && !env.isEmpty()) {
            env.each { k, v ->
                container.addEnv(k, String.valueOf(v))
            }
        }

        Map<Integer, Integer> mappings = parseMappings(config.ports)
        if (config.useHostNetwork) {
            container.withNetworkMode('host')
            if (!mappings.isEmpty()) {
                environment.logger.warn('WARNING: Ignoring mapped ports {} configuration because container use host ' +
                        'network and so all container ports would be available on host directly (mapping not ' +
                        'possible). See python.docker.useHostNetwork option.', config.ports)
            }
        } else {
            mappings.each { k, v ->
                container.withFixedExposedPort(k, v)
            }
        }

        return container
    }

    private String getDockerWorkDir(String workDir) {
        workDir ? toDockerPath(environment.file(workDir).absolutePath) : projectDockerPath
    }

    private Map<Integer, Integer> parseMappings(Set<String> mappings) {
        Map<Integer, Integer> res = [:]
        if (mappings != null) {
            mappings.each {
                int src, target
                String[] split = it.split(':')
                src = Integer.valueOf(split[0])
                target = split.size() > 1 ? Integer.valueOf(split[1]) : src
                res[src] = target
            }
        }
        return res
    }

    /**
     * Docker container configuration object used for container startup and configuration changes tracking.
     */
    static class Config {
        DockerConfig docker
        String workDir
        Map<String, Object> env

        Config(DockerConfig docker, String workDir, Map<String, Object> env) {
            this.docker = docker
            this.workDir = workDir
            this.env = env
        }

        /**
         * Compares two configuration objects (to detect when container must be restarted).
         *
         * @param conf new configuration
         * @return detected change description or null if no changes
         */
        @SuppressWarnings('IfStatementCouldBeTernary')
        String diff(Config conf) {
            // work dir changed
            if (workDir != conf.workDir) {
                return 'working directory'
            }
            // environment changed (check that all variables already present in current container)
            if (conf.env && (env == null || conf.env.find { k, v -> env[k] != v })) {
                return 'environment variables'
            }
            if (conf.docker.ports &&
                    (!docker.ports || !docker.ports.containsAll(conf.docker.ports))) {
                return 'ports'
            }
            return null
        }
    }
}
