package ru.vyarus.gradle.plugin.python.task

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Python

/**
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
class PipInstallTask extends ConventionTask {

    @Input
    @Optional
    String pythonPath
    @Input
    @Optional
    List<String> modules = []
    @Input
    @Console
    boolean showInstalledVersions
    /**
     *
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
        logger.lifecycle("Using python: {}", python.homeDir)

        Pip pip = new Pip(project, getPythonPath())
        List<PipModule> mods = resolveModules()
        if (!mods.isEmpty()) {
            String installed = (isAlwaysInstallModules()
                    ? '' : python.readOutput('-m pip freeze')).toLowerCase()
            boolean altered
            // install modules
            mods.each { PipModule mod ->
                String pipDef = mod.toPipString()
                // don't install if already installed (assume dependencies are also installed)
                if (!installed.contains(pipDef.toLowerCase())) {
                    pip.install(pipDef)
                    altered = true
                }
            }
            if (!isAlwaysInstallModules() && !altered) {
                logger.lifecycle("All required modules are already installed")
            }
        }
        if (isShowInstalledVersions()) {
            // show all installed modules versions (to help problems resolution)
            pip.exec('list --format=columns')
        }
    }

    void pip(String... modules) {
        pip(Arrays.asList(modules))
    }

    void pip(Iterable<String> modules) {
        getModules().addAll(modules)
    }

    /**
     * Resolve modules list for installation by removing duplicate definitions (the latest definition wins).
     *
     * @return pip modules to install
     */
    private List<PipModule> resolveModules() {
        Map<String, PipModule> mods = new LinkedHashMap<>()
        // sequential parsing in order to override duplicate definitions
        // (latter defined module overrides previous definition) and preserve definition order
        getModules().each {
            PipModule mod = PipModule.parse(it)
            mods[mod.name] = mod
        }
        return new ArrayList(mods.values())
    }
}
