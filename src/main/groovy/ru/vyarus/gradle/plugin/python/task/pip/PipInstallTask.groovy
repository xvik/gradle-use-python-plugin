package ru.vyarus.gradle.plugin.python.task.pip

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.tasks.*

/**
 * Install required modules (with correct versions) into python using pip.
 * Default task is registered as pipInstall to install all modules declared in
 * {@link ru.vyarus.gradle.plugin.python.PythonExtension#modules}.
 * {@link ru.vyarus.gradle.plugin.python.task.CheckPythonTask} always run before pip install task to validate
 * environment.
 * <p>
 * All {@link ru.vyarus.gradle.plugin.python.task.PythonTask}s are depend on pipInstall by default.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
class PipInstallTask extends BasePipTask {

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
    boolean alwaysInstallModules

    /**
     * Extra {@code pip install} arguments not covered directly by api.
     * @see <a href="https://pip.pypa.io/en/stable/reference/pip_install/#options">options</a>
     */
    @Input
    @Optional
    List<String> options = []

    PipInstallTask() {
        onlyIf { !modulesList.empty }
        // task will always run for the first time (even if deps are ok), but all consequent runs will be up-to-date
        outputs.upToDateWhen { modulesToInstall.empty }
    }

    @TaskAction
    void run() {
        pip.python.extraArgs(getOptions())
        modulesToInstall.each { pip.install(it) }
        // apply options only for install calls! otherwise, following pip calls will fail
        pip.python.clearExtraArgs()

        // could be at first run (upToDateWhen requires at least one task execution)
        if (modulesToInstall.empty) {
            logger.lifecycle('All required modules are already installed with correct versions')
        }

        if (isShowInstalledVersions()) {
            // show all installed modules versions (to help problems resolution)
            // note: if some modules are already installed in global scope and user scope is used,
            // then global modules will not be shown
            pip.exec('list --format=columns')
        }
    }

    /**
     * Add extra {@code pip install} options, applied to command.
     *
     * @param args arguments
     */
    @SuppressWarnings('ConfusingMethodName')
    void options(String... args) {
        if (args) {
            getOptions().addAll(args)
        }
    }

    @Internal
    protected List<String> getModulesToInstall() {
        buildModulesToInstall()
    }

    @Memoized
    private List<String> buildModulesToInstall() {
        List<String> res = []
        if (!modulesList.empty) {
            // use list of installed modules to check if 'pip install' is required for module
            // have to always use global list (even if user scope used) to avoid redundant installation attempts
            List<String> installed = (isAlwaysInstallModules() ? ''
                    : pip.inGlobalScope { pip.readOutput('freeze') } as String).toLowerCase().readLines()
            // install modules
            modulesList.each { PipModule mod ->
                // don't install if already installed (assume dependencies are also installed)
                if (!installed.contains(mod.toPipString().toLowerCase())) {
                    logger.info('Required pip module not installed: {}', mod)
                    res.add(mod.toPipInstallString())
                }
            }
        }
        return res
    }
}
