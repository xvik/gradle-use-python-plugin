package ru.vyarus.gradle.plugin.python.cmd.docker

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.Container
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import ru.vyarus.gradle.plugin.python.util.DurationFormatter

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ExecutionException

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
@SuppressWarnings('SynchronizedMethod')
@CompileStatic
class ContainerManager {

    private static final String DOCKER_PROJECT_PATH = '/usr/source/'
    private static final String DOCKER_WINDOWS_PROJECT_PATH = 'c:/projects/'
    private static final byte[] EMPTY = new byte[0]
    private static final String NL = '\n'

    private final String image
    private final boolean windowsImage
    private final Project project
    private final String projectRootPath
    private final String projectDockerPath

    private PythonContainer container
    private Config containerConfig

    ContainerManager(String image, boolean windows, Project project) {
        this.image = image
        this.windowsImage = windows
        this.project = project
        this.projectRootPath = project.rootProject.projectDir.absolutePath
        this.projectDockerPath =
                (windows ? DOCKER_WINDOWS_PROJECT_PATH : DOCKER_PROJECT_PATH) + project.rootProject.name
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
     *
     * @param command command to convert paths in
     */
    void convertCommand(String[] command) {
        for (int i = 0; i < command.length; i++) {
            String cmd = command[i]
            if (cmd.startsWith(projectRootPath)) {
                command[i] = toDockerPath(cmd)
            }
        }
    }

    /**
     * Converts path to docker path, according to mapping (only for absolute paths, leading inside project).
     *
     * @param path path to convert
     * @return converted path (or not changed, if conversion not require)
     * @see #toLocalPath(java.lang.String) reverse conversion
     */
    String toDockerPath(String path) {
        path.replace(projectRootPath, projectDockerPath)
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
     * @param environment environment variables (may be null)
     */
    synchronized void restartIfRequired(DockerConfig config, String workDir, Map<String, Object> environment) {
        Config upd = new Config(config, workDir, environment)
        if (container == null) {
            start(upd)
        } else {
            // compare started container configuration
            String diff = containerConfig.diff(upd)
            if (diff) {
                project.logger.lifecycle('Restarting container due to changed {}', diff)
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
     * Stops started container or do nothing if not started.
     */
    synchronized void stop() {
        if (container && container.running) {
            long watch = System.currentTimeMillis()
            String name = container.containerName
            try {
                container.stop()
            } finally {
                container = null
                containerConfig = null
            }
            project.logger.lifecycle('[docker] container \'{}\' ({}) stopped in {}',
                    image, name, DurationFormatter.format(System.currentTimeMillis() - watch))
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
        project.logger.info('Command executed in pre-started container ({}) in {}',
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
    int execExclusive(String[] command,
                      OutputStream out,
                      DockerConfig config,
                      String workDir,
                      Map<String, Object> env) {
        long watch = System.currentTimeMillis()
        PythonContainer cont = createContainer(config, workDir, env)
                .withCommand(command)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        // output live stream
                .withLogConsumer { OutputFrame frame -> out.write(frame.bytes ?: EMPTY) }

        try {
            cont.start()
            project.logger.info('Command executed in exclusive container ({}) in {}',
                    container.containerName, DurationFormatter.format(System.currentTimeMillis() - watch))
            return 0
        } catch (ExecutionException ex) {
            // normally it would not be visible, but it would be possible to see it with --info flag
            project.logger.info('Exclusive container startup failed', ex)
            // real error code is not known!
            return 1
        } finally {
            cont.stop()
        }
    }

    @SuppressWarnings('UnusedMethodParameter')
    String formatContainerInfo(DockerConfig config, String workDir, Map<String, Object> env) {
        StringBuilder res = new StringBuilder()
        String labelPattern = '\t%-15s '
        res.append(String.format(labelPattern, 'Mount'))
                .append(projectRootPath).append(':').append(projectDockerPath).append(NL)
        res.append(String.format(labelPattern, 'Work dir'))
                .append(getDockerWorkDir(workDir)).append(NL)
        if (env != null && !env.empty) {
            res.append(String.format(labelPattern, "Env (${env.size()})"))
            // only names because there might be secrets in values
                    .append(env.keySet().join(', ')).append(NL)
        }
        return res.toString()
    }

    @SuppressWarnings('Indentation')
    private void start(Config config) {
        if (container != null) {
            return
        }
        long watch = System.currentTimeMillis()
        container = createContainer(config.docker, config.workDir, config.env)
        // infinite process to keep container running
        if (windows) {
            container.withCommand('ping', '-t', 'localhost', '>', 'NUL')
        } else {
            container.withCommand('tail', '-f', '/dev/null')
        }
        container.withStartupTimeout(Duration.ofSeconds(1))
                .withLogConsumer { OutputFrame frame ->
                    if (frame.bytes != null) {
                        project.logger.lifecycle('[docker{}] {}', container.containerName, frame.utf8String)
                    }
                }
        container.start()
        containerConfig = config
        project.logger.lifecycle('[docker] container \'{}\' ({}) started in {}\n{}',
                image, container.containerName, DurationFormatter.format(System.currentTimeMillis() - watch),
                formatContainerInfo(config.docker, config.workDir, config.env))
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private PythonContainer createContainer(DockerConfig config, String workDir, Map<String, Object> env) {
        PythonContainer container = new PythonContainer(image)
                .withFileSystemBind(projectRootPath, projectDockerPath, BindMode.READ_WRITE)
                .withWorkingDirectory(getDockerWorkDir(workDir))

        if (env != null && !env.empty) {
            env.each { k, v ->
                container.addEnv(k, String.valueOf(v))
            }
        }

//        Testcontainers.exposeHostPorts()
//                .withAccessToHost(true)

        // image pull policy
        //https://www.testcontainers.org/features/advanced_options/
        return container
    }

    private String getDockerWorkDir(String workDir) {
        workDir ? toDockerPath(project.file(workDir).absolutePath) : projectDockerPath
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
            return null
        }
    }
}
