package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.task.pip.module.ModuleFactory

/**
 * Use-python plugin extension. Used to declare python location (use global if not declared) and specify
 * required modules to install.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PythonExtension {

    /**
     * Path to python binary (folder where python executable is located). When not set, global python is called.
     * NOTE: automatically set when virtualenv is used (see {{@link #scope}).
     */
    String pythonPath
    /**
     * Python binary name to use. By default, for linux it would be python3 (if installed) or python and
     * for windows - python.
     * <p>
     * Useful when you want to force python 2 usage on linux or python binary name differs.
     */
    String pythonBinary

    /**
     * Minimal required python version. Full format: "major.minor.micro". Any precision could be used, for example:
     * '3' (python 3 or above), '3.2' (python. 3.2 or above), '3.6.1' (python 3.6.1 or above).
     */
    String minPythonVersion
    /**
     * Minimal required pip version. Format is the same as python version: "major.minor.micro". Any precision could
     * be used (9, 8.2, etc). By default pip 9 is required.
     */
    String minPipVersion = '9'

    /**
     * Pip modules to install. Modules are installed in the declaration order.
     * Duplicates are allowed: only the latest declaration will be used. So it is possible
     * to pre-define some modules (for example, in plugin) and then override module version
     * by adding new declaration (with {@code pip ( .. )} method).
     * <p>
     * Use {@code pip ( .. )} methods for modules declaration.
     */
    List<String> modules = []

    /**
     * Calls 'pip list' to show all installed python modules (for problem investigations).
     * Enabled by default for faster problems detection (mostly to highlight used transitive dependencies versions).
     */
    boolean showInstalledVersions = true

    /**
     * By default, plugin will not call "pip install" for modules already installed (with exactly
     * the same version). Enable option if you need to always call "pip install module" (for example,
     * to make sure correct dependencies installed).
     */
    boolean alwaysInstallModules

    /**
     * By default, pip cache all installed packages. Sometimes this may lead to incorrect dependency version
     * resolution (when newer version from cache installed instead of specified). Since pip 20 vcs dependencies
     * (module build from vcs revision) are also cached, so if you need to force rebuild each time you will
     * need to disable cache (may be useful for testing).
     * <p>
     * Essentially, disabling this option adds {@code --no-cache-dir} for all pip calls.
     *
     * @see <a href="https://pip.pypa.io/en/stable/reference/pip_install/#caching">--no-cache-dir</a>
     */
    boolean usePipCache = true

    /**
     * This can be used to if you host your own pypi besides the default global one.
     * Applies to {@code pip install}, {@code pip download}, {@code pip list} and {@code pip wheel}.
     *
     * @see <a href="https://pip.pypa.io/en/stable/reference/pip_install/#install-extra-index-url">--extra-index-url</a>
     */
    List<String> extraIndexUrls

    /**
     * Mark a host as trusted for pip even when it has no tls, or the certificate is invalid.
     * Can be used in combination with {@link #extraIndexUrls} to use your own pypi server.
     * Applies only for {@code pip install} (other commends does not support this option).
     *
     * @see <a href="https://pip.pypa.io/en/stable/reference/pip/#trusted-host">--trusted-host</a>
     */
    List<String> trustedHosts

    /**
     * Target scope for pip packages installation.
     */
    Scope scope = Scope.VIRTUALENV_OR_USER
    /**
     * Automatically install virtualenv (if pip modules used). Will install for current user only (--user).
     * Installed virtualenv will not be modified later, so you will need to manually update it in case of problems.
     * When disabled, plugin will still check if virtualenv is available, but will not try to install it if not.
     */
    boolean installVirtualenv = true
    /**
     * Used only when virtualenv is installed automatically ({@link #installVirtualenv). It's better to install
     * exact version, known to work well to avoid side effects. For example, virtualenv 20.0.x is a major rewrite and
     * it contains some minor regressions in behaviour (which would be fixed eventually, ofc).
     * <p>
     * You can set null or empty value to install the latest version.
     * <p>
     * Property not used at all if virtualenv is already installed (no warning on version mismatch): if you installed
     * it manually then you know what you are doing, otherwise "known to be working" version should be installed to
     * avoid problems.
     */
    String virtualenvVersion = '16.7.9'
    /**
     * Virtual environment path to use. Used only when {@link #scope} is configured to use virtualenv.
     * Virtualenv will be created automatically if not yet exists.
     * <p>
     * By default env path is set to '.gradle/python' inside the root project (!). This avoids virtualenv duplication
     * in the multi-module setup (all modules will use the same env and so pip modules may be installed only once).
     * <p>
     * If multiple env required (e.g. to use different python versions or to separate pip initialization (e.g.
     * for incompatible modules)) declare env path manually in each module.
     */
    String envPath
    /**
     * Copy virtual environment instead of symlink (see --always-copy virtualenv option).
     * By default use default virtualenv behaviour: symlink environment.
     */
    boolean envCopy

    PythonExtension(Project project) {
        // by default storing environment inside the root project and use relative path (for simpler logs)
        envPath = project.relativePath(project.rootProject.file('.gradle/python'))
    }

    /**
     * Shortcut for {@link #pip(java.lang.Iterable)}.
     *
     * @param modules pip modules to install
     */
    void pip(String... modules) {
        pip(Arrays.asList(modules))
    }

    /**
     * Declare pip modules to install. Format: "moduleName:version". Only exact version
     * matching is supported (pip support ranges, but this is intentionally not supported in order to avoid
     * side effects).
     * <p>
     * Feature syntax is also allowed: "moduleName[qualifier]:version". As it is impossible to detect enabled features
     * of installed modules then qualified module will not be installed if module with the same name and
     * version is already installed (moduleName:version).
     * <p>
     * VCS module declaration is also supported in format: "vcs+protocol://repo_url/@vcsVersion#egg=pkg-pkgVersion".
     * Note that it requires both vcs version (e.g. commit hash) and package version in order to be able to perform
     * up-to-date check. You can specify branch name or tag (for example, for git) instead of direct hash, but
     * prefer using exact version (hash or tags, not branches) in order to get reproducible builds.
     * See <a href="https://pip.pypa.io/en/stable/reference/pip_install/#vcs-support">pip vcs docs</p>.
     * <p>
     * Duplicate declarations are allowed: in this case the latest declaration will be used.
     * <p>
     * Note: if newer package version is already installed, pip will remove it and install correct version.
     *
     * @param modules pip modules to install
     */
    void pip(Iterable<String> modules) {
        this.modules.addAll(modules)
    }

    /**
     * Pip dependencies installation scope.
     */
    static enum Scope {
        /**
         * Global scope (all users). May require additional rights on linux.
         */
        GLOBAL,
        /**
         * Current user (os) only. Packages are installed into the user specific (~/) directory.
         */
        USER,
        /**
         * Same behaviour as {@link #VIRTUALENV} when virtualenv module installed (possibly automatically), but if
         * module not found, falls back to user scope (same as {@link #USER}, instead of creating local environment).
         */
        VIRTUALENV_OR_USER,
        /**
         * Use virtualenv. virtualenv must be either installed manually or requested for automatic installation
         * {@link PythonExtension#installVirtualenv}). Will fail if mode not installed (either way).
         * If virtualenv is not created, then it would be created automatically.
         * Path of environment location is configurable (by default cached in .gradle dir).
         */
        VIRTUALENV
    }

    /**
     * Checks if module is already declared (as usual or vcs module). Could be used by other plugins to test
     * if required module is manually declared or not (in order to apply defaults).
     *
     * @param name module name to check
     * @return true of module already declared, false otherwise
     */
    boolean isModuleDeclared(String name) {
        return ModuleFactory.findModuleDeclaration(name, modules) != null
    }
}
