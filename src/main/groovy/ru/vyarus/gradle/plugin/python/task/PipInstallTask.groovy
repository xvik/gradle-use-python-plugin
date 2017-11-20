package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Python

/**
 * Install required modules (with correct versions) into python using pip.
 * Default task is registered as pipInstall to install all modules declared in
 * {@link ru.vyarus.gradle.plugin.python.PythonExtension#modules}.
 * All {@link PythonTask}s are depend on pipInstall by default.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PipInstallTask extends ConventionTask {

    /**
     * Path to directory with python binary. When not set global python is used.
     * By default use {@link ru.vyarus.gradle.plugin.python.PythonExtension#pythonPath} value.
     */
    @Input
    @Optional
    String pythonPath
    /**
     * List of modules to install. Module declaration format: 'name:version'.
     * For default pipInstall task modules are configured in
     * {@link ru.vyarus.gradle.plugin.python.PythonExtension#modules}
     */
    @Input
    @Optional
    List<String> modules = []
    /**
     * True to show list of all installed python modules (not only modules installed by plugin!).
     * By default use {@link ru.vyarus.gradle.plugin.python.PythonExtension#showInstalledVersions} value.
     */
    @Console
    boolean showInstalledVersions
    /**
     * True to always call 'pip install' for configured modules, otherwise pip install called only
     * if module is not installed or different version installed.
     * By default use {@link ru.vyarus.gradle.plugin.python.PythonExtension#showInstalledVersions} value.
     */
    @Input
    @Optional
    boolean alwaysInstallModules

    PipInstallTask() {
        group = 'python'
    }

    @TaskAction
    void run() {
        Python python = new Python(project, getPythonPath())
        logger.lifecycle('Using python: {}', python.homeDir)

        Pip pip = new Pip(project, getPythonPath())
        List<PipModule> mods = resolveModules()
        if (!mods.isEmpty()) {
            // use list of installed modules to check if 'pip install' is required for module
            List<String> installed = (isAlwaysInstallModules()
                    ? '' : python.readOutput('-m pip freeze')).toLowerCase().readLines()
            boolean altered = false
            // install modules
            mods.each { PipModule mod ->
                String pipDef = mod.toPipString()
                // don't install if already installed (assume dependencies are also installed)
                if (!installed.contains(pipDef.toLowerCase())) {
                    pip.install(pipDef)
                    altered = true
                }
            }
            if (!altered) {
                logger.lifecycle('All required modules are already installed with correct versions')
            }
            // only if there are any required modules, otherwise no need
            if (isShowInstalledVersions()) {
                // show all installed modules versions (to help problems resolution)
                pip.exec('list --format=columns')
            }
        }
    }

    /**
     * Shortcut for {@link #pip(java.lang.Iterable)}.
     *
     * @param modules modules to install
     */
    void pip(String... modules) {
        pip(Arrays.asList(modules))
    }

    /**
     * Add modules to install. Module format: 'name:version'. Duplicate declarations are allowed: in this case the
     * latest declaration will be used.
     *
     * @param modules modules to install
     */
    void pip(Iterable<String> modules) {
        getModules().addAll(modules)
    }

    /**
     * Resolve modules list for installation by removing duplicate definitions (the latest definition wins).
     *
     * @return pip modules to install
     */
    private List<PipModule> resolveModules() {
        Map<String, PipModule> mods = [:] // linked map
        // sequential parsing in order to override duplicate definitions
        // (latter defined module overrides previous definition) and preserve definition order
        getModules().each {
            PipModule mod = PipModule.parse(it)
            mods[mod.name] = mod
        }
        return new ArrayList(mods.values())
    }
}
