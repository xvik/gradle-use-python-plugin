package ru.vyarus.gradle.plugin.python.cmd.docker

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.helpers.NOPLoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.InternetProtocol
import org.testcontainers.utility.DockerImageName

/**
 * Special class required to tune default {@link GenericContainer} behaviour.
 *
 * @author Vyacheslav Rusakov
 * @since 28.09.2022
 */
@CompileStatic
class PythonContainer extends GenericContainer<PythonContainer> {

    PythonContainer(String image) {
        super(DockerImageName.parse(image))
    }

    // require only because groovy can't compile otherwise
    @SuppressWarnings(['CloseWithoutCloseable', 'UnnecessaryOverridingMethod'])
    @Override
    void close() {
        super.close()
    }

    // from deprecated FixedHostPortGenericContainer
    // we can't use random ports here because it would require additional api for exposing mappings
    // and would be completely unusable for exclusive containers

    /**
     * Bind a fixed TCP port on the docker host to a container port
     * @param hostPort          a port on the docker host, which must be available
     * @param containerPort     a port in the container
     * @return                  this container
     */
    PythonContainer withFixedExposedPort(int hostPort, int containerPort) {
        return withFixedExposedPort(hostPort, containerPort, InternetProtocol.TCP)
    }

    /**
     * Bind a fixed port on the docker host to a container port
     * @param hostPort          a port on the docker host, which must be available
     * @param containerPort     a port in the container
     * @param protocol          an internet protocol (tcp or udp)
     * @return                  this container
     */
    PythonContainer withFixedExposedPort(int hostPort, int containerPort, InternetProtocol protocol) {
        super.addFixedExposedPort(hostPort, containerPort, protocol)
        return self()
    }

    @Override
    @SuppressWarnings('UnnecessaryGetter')
    protected Logger logger() {
        // avoid direct logging of errors (prevent duplicates in log)
        return NOPLoggerFactory.getConstructor().newInstance().getLogger(PythonContainer.name)
    }
}
