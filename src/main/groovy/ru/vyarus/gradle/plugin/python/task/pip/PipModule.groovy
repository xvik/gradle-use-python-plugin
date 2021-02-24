package ru.vyarus.gradle.plugin.python.task.pip

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.task.pip.module.ModuleFactory

/**
 * Pip module declaration pojo. Support parsing 'name:version' format (used for configuration).
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PipModule {
    String name
    String version

    /**
     * Parse module declaration in format 'module:version' or 'vcs+protocol://repo_url/@vcsVersion#egg=pkg-pkgVersion'
     * (for vcs module).
     *
     * @param declaration module declaration to parse
     * @return parsed module pojo
     * @throws IllegalArgumentException if module format does not match
     * @see ModuleFactory#create(java.lang.String)
     */
    static PipModule parse(String declaration) {
        return ModuleFactory.create(declaration)
    }

    PipModule(String name, String version) {
        if (!name) {
            throw new IllegalArgumentException('Module name required')
        }
        if (!version) {
            throw new IllegalArgumentException('Module version required')
        }

        this.name = name
        this.version = version
    }

    /**
     * @return human readable module declaration
     */
    @Override
    String toString() {
        return "$name $version"
    }

    /**
     * Must be used for module up to date detection.
     *
     * @return module declaration in pip format
     * @deprecated freeze command output changed in pip 21 for vcs modules and so now exact pip version is required
     * for proper up-to-date check
     */
    @Deprecated
    String toPipString() {
        return toFreezeStrings()[0]
    }

    /**
     * Module record as it appears in {@code pip freeze} command.
     * Must be used for module up to date detection. Multiple results required to properly support
     * changed pip output syntax between versions.
     *
     * @param pipVersion current pip version (because command output could change)
     * @return list of possible module declarations in the same format as freeze will print
     */
    List<String> toFreezeStrings() {
        // exact version matching!
        // pip will re-install even newer package to an older version
        return ["$name==$version" as String]
    }

    /**
     * Must be used for installation.
     *
     * @return module installation declaration
     */
    String toPipInstallString() {
        return "$name==$version"
    }

    boolean equals(Object o) {
        if (this.is(o)) {
            return true
        }
        if (!getClass().isAssignableFrom(o.class)) {
            return false
        }

        PipModule pipModule = (PipModule) o
        return name == pipModule.name && version == pipModule.version
    }

    int hashCode() {
        int result
        result = name.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}
