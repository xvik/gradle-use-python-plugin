package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerConfig
import ru.vyarus.gradle.plugin.python.util.CliUtils

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Pip commands execution utility. Use {@link Python} internally.
 *
 * @author Vyacheslav Rusakov
 * @since 13.12.2017
 */
@CompileStatic
class Virtualenv {

    private static final Pattern VERSION = Pattern.compile('virtualenv ([\\d.]+)')

    public static final String PIP_NAME = 'virtualenv'

    private final Project project
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
     * @param validateSystemBinary validate global python binary
     * @param path environment path (relative to project or absolute)
     */
    Virtualenv(Project project, String pythonPath, String binary, String path) {
        this.project = project
        python = new Python(project, pythonPath, binary).logLevel(LogLevel.LIFECYCLE)
        this.path = path
        if (!path) {
            throw new IllegalArgumentException('Virtualenv path not set')
        }
        location = project.file(path)
    }

    /**
     * System binary search is performed only for global python (when pythonPath is not specified). Enabled by default.
     *
     * @param validate true to search python binary in system path and fail if not found
     * @return cli instance for chained calls
     */
    Virtualenv validateSystemBinary(boolean validate) {
        this.python.validateSystemBinary(validate)
        return this
    }

    /**
     * Enable docker support: all python commands would be executed under docker container.
     *
     * @param docker docker configuration (may be null)
     * @return cli instance for chained calls
     */
    Virtualenv withDocker(DockerConfig docker) {
        this.python.withDocker(docker)
        return this
    }

    /**
     * Shortcut for {@link Python#workDir(java.lang.String)}.
     *
     * @param workDir python working directory
     * @return virtualenv instance for chained calls
     */
    Virtualenv workDir(String workDir) {
        python.workDir(workDir)
        return this
    }

    /**
     * Shortcut for {@link Python#environment(java.util.Map)}.
     *
     * @param env environment map
     * @return pip instance for chained calls
     */
    Virtualenv environment(Map<String, Object> env) {
        python.environment(env)
        return this
    }

    /**
     * Perform pre-initialization and, if required, validate global python binary correctness. Calling this method is
     * NOT REQUIRED: initialization will be performed automatically before first execution. But it might be called
     * in order to throw possible initialization error before some other logic (related to exception handling).
     *
     * @return virtualenv instance for chained calls
     */
    Virtualenv validate() {
        python.validate()
        return this
    }

    /**
     * @return virtualenv version (major.minor.micro)
     */
    @Memoized
    String getVersion() {
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

    /**
     * @return virtualenv --version output
     */
    @Memoized
    String getVersionLine() {
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

    /**
     * @return true if virtualenv exists
     */
    boolean exists() {
        return location.exists() && location.list().size() > 0
    }

    /**
     * Create virtualenv with setuptools and pip. Do nothing if already exists.
     * To copy environment instead of symlinking, use {@code copy (true)} otherwise don't specify parameter.
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
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

    /**
     * @return python path to use for environment
     */
    @Memoized
    String getPythonPath() {
        String res = CliUtils.pythonBinPath(location.absolutePath, python.windows)
        return Paths.get(path).absolute ? res
                // use shorter relative path
                : project.relativePath(res)
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
