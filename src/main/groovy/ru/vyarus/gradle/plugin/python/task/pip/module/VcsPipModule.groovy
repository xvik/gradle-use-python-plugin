package ru.vyarus.gradle.plugin.python.task.pip.module

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.task.pip.PipModule
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * Supported vsc module format: vcs+protocol://repo_url/@vcsVersion#egg=pkg-pkgVersion. It requires both commit version
 * (may be tag or branch name) and package version. Package version is important because otherwise it would be
 * impossible to track up-to date state without python run (slow). Vsc version is important for predictable
 * builds.
 * <p>
 * IMPORTANT: if you specify branch version then module will be installed only once because it will rely on
 * version declaration in #egg part. The only way to workaround it is to use
 * {@code python.alwaysInstallModules = true} option in order to delegate dependency management to pip.
 * <p>
 * Note: egg=project-version is official convention, but it is not supported, so version part is cut off in actual
 * pip install command.
 *
 * @author Vyacheslav Rusakov
 * @since 18.05.2018
 * @see <a href="https://pip.pypa.io/en/stable/reference/pip_install/#vcs-support">pip vsc support</a>
 */
@CompileStatic
class VcsPipModule extends PipModule {

    private final String declaration

    VcsPipModule(String declaration, String name, String version) {
        super(name, version)
        this.declaration = declaration
    }

    @Override
    String toString() {
        return "$name $version ($declaration)"
    }

    @Override
    String toPipInstallString() {
        return declaration
    }

    @Override
    String toFreezeString(String pipVersion) {
        if (CliUtils.isVersionMatch(pipVersion, '21')) {
            // In pip 21 freeze command shows exact version path, instead of pure version!

            // Separator would be always present due to forced validation in
            // ru.vyarus.gradle.plugin.python.task.pip.module.ModuleFactory.parseVcsModule
            String hash = declaration[0..declaration.lastIndexOf('#') - 1]
            return "$name @ $hash"
        }
        // older pips didn't differentiate vcs modules
        return super.toFreezeString(pipVersion)
    }
}
