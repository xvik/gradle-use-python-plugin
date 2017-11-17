package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic

/**
 * use-python plugin extension.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PythonExtension {

    /**
     * Path to python binary.
     */
    String pythonPath
    /**
     * Pip modules to install
     */
    List<String> modules = []

    /**
     * Calls 'pip list' to show all installed python modules (for problem investigations).
     */
    boolean showInstalledVersions = true

    /**
     * By default, plugin will not call "pip install" for modules already installed (with exactly
     * the same version). Enable option if you need to always call "pip install module" .
     */
    boolean alwaysInstallModules

    void pip(String... modules) {
        pip(Arrays.asList(modules))
    }

    void pip(Iterable<String> modules) {
        this.modules.addAll(modules)
    }
}
