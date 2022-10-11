package ru.vyarus.gradle.plugin.python.cmd.docker

import groovy.transform.CompileStatic

/**
 * Docker configuration for python execution. For documentation see plugin extension object
 * {@link ru.vyarus.gradle.plugin.python.PythonExtension#docker} or task docker configuration object
 * {@link ru.vyarus.gradle.plugin.python.task.BasePythonTask.DockerEnv}.
 * <p>
 * Note that such triple duplication of docker configuration objects is required for better customization and
 * ability for direct {@link ru.vyarus.gradle.plugin.python.cmd.Python} (and related) object usage.
 *
 * @author Vyacheslav Rusakov
 * @since 27.09.2022
 */
@CompileStatic
class DockerConfig {

    boolean windows
    String image
    boolean exclusive
    Set<String> ports
}
