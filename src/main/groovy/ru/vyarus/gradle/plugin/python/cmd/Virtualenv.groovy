package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.cmd.env.Environment

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Virtualenv commands execution utility. Use {@link Python} internally.
 *
 * @author Vyacheslav Rusakov
 * @since 13.12.2017
 */
@CompileStatic
class Virtualenv extends VirtualTool<Virtualenv> {

    private static final Pattern VERSION = Pattern.compile('virtualenv ([\\d.]+)')

    public static final String PIP_NAME = 'virtualenv'

    // module name
    final String name = PIP_NAME

    Virtualenv(Environment environment, String path) {
        this(environment, null, null, path)
    }

    /**
     * Create virtualenv utility.
     *
     * @param environment gradle api access object
     * @param pythonPath python path (null to use global)
     * @param binary python binary name (null to use default python3 or python)
     * @param path environment path (relative to project or absolute)
     */
    Virtualenv(Environment environment, String pythonPath, String binary, String path) {
        super(environment, pythonPath, binary, path)
    }

    /**
     * @return virtualenv version (major.minor.micro)
     */
    String getVersion() {
        return python.getOrCompute('virtualenv.version') {
            // first try to parse line to avoid duplicate python call
            Matcher matcher = VERSION.matcher(versionLine)
            if (matcher.find()) {
                // note: this will drop beta postfix (e.g. for 10.0.0b2 version will be 10.0.0)
                return matcher.group(1)
            }
            // if can't recognize version, ask directly
            return python.withHiddenLog {
                python.readOutput("-c \"import $name; print(${name}.__version__)\"")
            }
        }
    }

    /**
     * @return virtualenv --version output
     */
    String getVersionLine() {
        return python.getOrCompute('virtualenv.version.line') {
            // virtualenv 20 returns long version string including location path
            String res = python.withHiddenLog {
                python.readOutput("-m $name --version")
            }
            // virtualenv 16 and below return only raw version (backwards compatibility)
            if (!res.startsWith(name)) {
                res = "$name $res"
            }
            return res
        }
    }

    /**
     * Create virtualenv with setuptools and pip. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy (true)} otherwise don't specify parameter.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    @Override
    void create(boolean copy = false) {
        create(true, true, copy)
    }

    /**
     * Create the lightest env without setuptools and pip. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy (true)} otherwise don't specify parameter.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void createPythonOnly(boolean copy = false) {
        create(false, false, copy)
    }

    /**
     * Create virtualenv. Do nothing if already exists.
     * To copy environment instead if symlinking, use {@code copy (? , ? , true)} otherwise omit last parameter.
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

    @Override
    String toString() {
        return env.file(pythonPath).canonicalPath + " (virtualenv $version)"
    }
}
