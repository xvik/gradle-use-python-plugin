package ru.vyarus.gradle.plugin.python.task.pip.module

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.task.pip.PipModule

/**
 * Feature-enabled modules support. E.g. 'requests[socks,security]:2.18.4'.
 * Such declaration should install modified version of requests module. Everything in square brackets is simply
 * passed to module's install script as parameters.
 * <p>
 * As it is not possible to track exact variation of installed module, then module will not be installed if
 * default 'requests:2.18.4' is installed.
 *
 * @author Vyacheslav Rusakov
 * @since 23.05.2018
 */
@CompileStatic
class FeaturePipModule extends PipModule {

    private final String qualifier

    FeaturePipModule(String name, String qualifier, String version) {
        super(name, version)
        this.qualifier = qualifier
    }

    @Override
    String toString() {
        return "${name}[$qualifier] $version"
    }

    @Override
    String toPipInstallString() {
        return "${name}[$qualifier]==$version"
    }
}
