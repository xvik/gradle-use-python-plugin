package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic

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
     * Minimal required pip version. Format is teh same as python version: "major.minor.micro". Any precision could
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
     * Installed pip packages scope.
     */
    Scope scope = Scope.VIRTUALENV_OR_USER
    /**
     * Virtual environment path to use. Used only when {@link #scope} is configured to use virtualenv.
     * Virtualenv will be created automatically if not yet exists.
     */
    String envPath = '.gradle/python'

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
     * Pip dependencies scope.
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
         * Use virtualenv if installed or fall back to user scope.
         */
        VIRTUALENV_OR_USER,
        /**
         * Use virtualenv. Fail if virtualenv module not installed.
         * If virtualenv is not created, then it would be created automatically.
         * Path of environment location is configurable (by difault cached in .gradle dir).
         */
        VIRTUALENV
    }

    /**
     * Checks if module is already declared. Could be used by other plugins to test if required module
     * is manually declared or not (in order to apply defaults).
     *
     * @param name module name to check
     * @return true of module already declared, false otherwise
     */
    boolean isModuleDeclared(String name) {
        String nm = name.toLowerCase() + ':'
        String res = modules.find {
            it.toLowerCase().startsWith(nm)
        }
        return res != null
    }
}
