package ru.vyarus.gradle.plugin.python.task.pip

import groovy.transform.CompileStatic

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

    PipModule(String name, String version) {
        assert name != null
        assert version != null

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
     * @return module declaration in pip format
     */
    String toPipString() {
        // exact version matching!
        // pip will re-install even newer package to an older version
        return "$name==$version"
    }

    boolean equals(Object o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
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

    /**
     * Parse module declaration in format 'module:version'.
     *
     * @param declaration module declaration to parse
     * @return parsed module pojo
     * @throws IllegalArgumentException if module format does not match
     */
    static PipModule parse(String declaration) {
        String[] parts = declaration.split(':')
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Incorrect pip module declaration (must be 'module:version'): $declaration")
        }
        return new PipModule(parts[0].trim() ?: null, parts[1].trim() ?: null)
    }
}
