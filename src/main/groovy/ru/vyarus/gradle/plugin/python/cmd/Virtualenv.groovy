package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * Pip commands execution utility. Use {@link Python} internally.
 *
 * @author Vyacheslav Rusakov
 * @since 13.12.2017
 */
@CompileStatic
class Virtualenv {

    public static final String PIP_NAME = 'virtualenv'

    private final Python python

    // module name
    final String name = PIP_NAME
    final String path
    final File location

    Virtualenv(Project project, String path) {
        this(project, null, null, path)
    }

    /**
     * Create virtualenv utility.
     *
     * @param project gradle project instance
     * @param pythonPath python path (null to use global)
     * @param binary python binary name (null to use default python3 or python)
     * @param path environment path (relative to project or absolute)
     */
    Virtualenv(Project project, String pythonPath, String binary, String path) {
        python = new Python(project, pythonPath, binary)
                .logLevel(LogLevel.LIFECYCLE)
        this.path = path
        if (!path) {
            throw new IllegalArgumentException('Virtualenv path not set')
        }
        location = project.file(path)
    }

    /**
     * @return virtualenv version (major.minor.micro)
     */
    @Memoized
    String getVersion() {
        python.withHiddenLog {
            python.readOutput("-m $name --version")
        }
    }

    /**
     * @return true if virtualenv exists
     */
    boolean exists() {
        return location.exists() && location.list().size() > 0
    }

    /**
     * Create virtualenv with setuptools and pip. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy ( true )} otherwise don't specify parameter.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void create(boolean copy = false) {
        create(true, true, copy)
    }

    /**
     * Create the lightest env without setuptools and pip. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy ( true )} otherwise don't specify parameter.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void createPythonOnly(boolean copy = false) {
        create(false, false, copy)
    }

    /**
     * Create virtualenv. Do nothing if already exists.
     * To copy environment instead if symlinking, use {@code copy ( ? , ? , true )} otherwise omit last parameter.
     *
     * @param setuptools do not install setuptools (--no-setuptools)
     * @param pip do not install pip and wheel (--no-pip --no-wheel)
     * @param copy copy virtualenv instead if symlink (--always-copy)
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void create(boolean setuptools, boolean pip, boolean copy = false) {
        if (exists()) {
            return
        }
        String cmd = path
        if (copy) {
            cmd += ' --always-copy'
        }
        if (!setuptools) {
            cmd += ' --no-setuptools'
        }
        if (!pip) {
            cmd += ' --no-pip --no-wheel'
        }
        python.callModule(name, cmd)
    }

    /**
     * @return python path to use for environment
     */
    @Memoized
    String getPythonPath() {
        return CliUtils.pythonBinPath(path)
    }

    /**
     * May be used to apply additional virtualenv ({@link Python#extraArgs(java.lang.Object)}) or python
     * ({@link Python#pythonArgs(java.lang.Object)}) arguments.
     *
     * @return python cli instance used to execute commands
     */
    Python getPython() {
        return python
    }
}
