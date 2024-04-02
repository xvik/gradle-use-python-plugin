package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * Venv commands execution utility. Use {@link Python} internally.
 * <p>
 * Note: venv is not a pip-managed module!
 * <p>
 * Usually venv is bundled with python (since 3.3), but not always: for example, on ubuntu it is a separate package
 * python3-venv.
 * <p>
 * Tool does not provide its version.
 *
 * @author Vyacheslav Rusakov
 * @since 21.09.2023
 */
@CompileStatic
class Venv extends VirtualTool<Venv> {

    public static final String NAME = 'venv'

    // module name
    final String name = NAME

    Venv(Environment environment, String path) {
        this(environment, null, null, path)
    }

    /**
     * Create venv utility.
     *
     * @param environment gradle api access object
     * @param pythonPath python path (null to use global)
     * @param binary python binary name (null to use default python3 or python)
     * @param path environment path (relative to project or absolute)
     */
    Venv(Environment environment, String pythonPath, String binary, String path) {
        super(environment, pythonPath, binary, path)
    }

    /**
     * Create venv with pip. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy (true)} otherwise don't specify parameter.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    @Override
    void create(boolean copy = false) {
        create(true, copy)
    }

    /**
     * Create the lightest env without pip. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy (true)} otherwise don't specify parameter.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void createPythonOnly(boolean copy = false) {
        create(false, copy)
    }

    /**
     * Create venv. Do nothing if already exists.
     * To copy environment instead if symlinking, use {@code copy (? , ? , true)} otherwise omit last parameter.
     *
     * @param pip do not install pip (--without-pip)
     * @param copy copy virtualenv instead if symlink (--copies)
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void create(boolean pip, boolean copy) {
        if (exists()) {
            return
        }
        String cmd = path
        if (copy) {
            cmd += ' --copies'
        }
        if (!pip) {
            cmd += ' --without-pip'
        }
        python.callModule(name, cmd)
    }

    /**
     * On ubuntu venv module is installed as a separate package (python3-venv) and is not visible as pip module.
     * So the only way to check its existence is calling it directly.
     *
     * @return true if venv is present, false if not
     */
    boolean isInstalled() {
        return python.getOrCompute('venv.installed') {
            try {
                python.withHiddenLog {
                    python.callModule(name, '-h')
                }
                return true
            } catch (PythonExecutionFailed ignored) {
                return false
            }
        }
    }

    @Override
    String toString() {
        return env.file(pythonPath).canonicalPath + ' (venv)'
    }
}
