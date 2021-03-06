package ru.vyarus.gradle.plugin.python.task.pip

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.task.BasePythonTask

/**
 * Base task for pip tasks.
 *
 * @author Vyacheslav Rusakov
 * @since 01.12.2017
 */
@CompileStatic
class BasePipTask extends BasePythonTask {

    /**
     * List of modules to install. Module declaration format: 'name:version'.
     * For default pipInstall task modules are configured in
     * {@link ru.vyarus.gradle.plugin.python.PythonExtension#modules}
     */
    @Input
    @Optional
    List<String> modules = []

    /**
     * Work with packages in user scope (--user pip option). When false - work with global scope.
     * Note that on linux it is better to work with user scope to overcome permission problems.
     * <p>
     * Enabled by default (see {@link ru.vyarus.gradle.plugin.python.PythonExtension#scope})
     */
    @Input
    boolean userScope

    /**
     * Affects only {@code pip install} by applying {@code --no-cache-dir}. Disables cache during package resolution.
     * May be useful in problematic cases (when cache leads to incorrect version installation or to force vcs
     * modules re-building each time (pip 20 cache vcs resolved modules by default)).
     * <p>
     * Enabled by default (see {@link ru.vyarus.gradle.plugin.python.PythonExtension#usePipCache})
     */
    @Input
    boolean useCache = true

    /**
     * Affects only {@code pip install} by applying {@code --trusted-host} (other pip commands does not support
     * this option).
     * <p>
     * No extra index urls are given by default (see
     * {@link ru.vyarus.gradle.plugin.python.PythonExtension#trustedHosts})
     */
    @Input
    @Optional
    List<String> trustedHosts = []

    /**
     * Affects only {@code pip install}, {@code pip download}, {@code pip list} and {@code pip wheel} by applying
     * {@code --extra-index-url}.
     * <p>
     * No extra index urls are given by default (see
     * {@link ru.vyarus.gradle.plugin.python.PythonExtension#extraIndexUrls})
     */
    @Input
    @Optional
    List<String> extraIndexUrls = []

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
    @Internal
    protected List<PipModule> getModulesList() {
        buildModulesList()
    }

    /**
     * @return configured pip utility instance
     */
    @Internal
    @SuppressWarnings('UnnecessaryGetter')
    protected Pip getPip() {
        buildPip()
    }

    @Memoized
    private List<PipModule> buildModulesList() {
        Map<String, PipModule> mods = [:] // linked map
        // sequential parsing in order to override duplicate definitions
        // (latter defined module overrides previous definition) and preserve definition order
        getModules().each {
            PipModule mod = PipModule.parse(it)
            mods[mod.name] = mod
        }
        return new ArrayList(mods.values())
    }

    @Memoized
    private Pip buildPip() {
        return new Pip(python, getUserScope(), getUseCache())
                .trustedHosts(getTrustedHosts())
                .extraIndexUrls(getExtraIndexUrls())
    }
}
