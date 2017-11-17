package ru.vyarus.gradle.plugin.python.task

/**
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
class PipModule {
    String name
    String version

    PipModule(String name, String version) {
        assert name != null
        assert version != null

        this.name = name
        this.version = version
    }

    @Override
    String toString() {
        return "$name $version"
    }

    String toPipString() {
        // exact version matching!
        // pip will re-install even newer package to an older version
        return "$name==$version"
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        PipModule pipModule = (PipModule) o

        if (name != pipModule.name) return false
        if (version != pipModule.version) return false

        return true
    }

    int hashCode() {
        int result
        result = name.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    static PipModule parse(String declaration) {
        String[] parts = declaration.split(':')
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Incorrect pip module declaration (must be 'module:version'): $declaration");
        }
        return new PipModule(parts[0].trim() ?: null, parts[1].trim() ?: null)
    }
}
