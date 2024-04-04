package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecException
import ru.vyarus.gradle.plugin.python.PythonExtension
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Python
import ru.vyarus.gradle.plugin.python.task.env.EnvSupport
import ru.vyarus.gradle.plugin.python.task.env.FallbackException
import ru.vyarus.gradle.plugin.python.task.env.VenvSupport
import ru.vyarus.gradle.plugin.python.task.env.VirtualenvSupport
import ru.vyarus.gradle.plugin.python.task.pip.BasePipTask
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * Task validates python installation. Will fail if python or pip not found or minimal version doesn't match.
 * Task called before any {@link ru.vyarus.gradle.plugin.python.task.BasePythonTask} (any python task) to
 * set validate and set correct python (use env).
 * <p>
 * If existing virtualenv is detected then plugin immediately switches to use it (without checking global python).
 * When virtualenv not exists, then global python checked. If pip modules required, pip existence checked and
 * then virtualenv existence checked (if scope allows). With default scope, when virtualenv module not found,
 * plugin fall back to os user dir. If virtualenv is strictly required (by scope) then build will fail.
 * <p>
 * When executing under docker it is required to re-use container between internal python calls, because
 * restarted container would lost its state, but we need to install virtualenv and create environment
 * (different commands).
 *
 * @author Vyacheslav Rusakov
 * @since 08.12.2017
 */
@CompileStatic
@SuppressWarnings('UnnecessaryGetter')
abstract class CheckPythonTask extends BasePipTask {

    private static final Object SYNC = new Object()
    private boolean virtual = false
    private boolean envCreated = false

    /**
     * Virtualenv scope
     */
    @Input
    abstract Property<PythonExtension.Scope> getScope()

    /**
     * Virtualenv folder path
     */
    @Input
    abstract Property<String> getEnvPath()

    /**
     * Minimal allowed python version
     */
    @Input
    @Optional
    abstract Property<String> getMinPythonVersion()

    /**
     * Minimal allowed pip version
     */
    @Input
    @Optional
    abstract Property<String> getMinPipVersion()

    /**
     * Use venv instead of virtualenv.
     */
    @Input
    abstract Property<Boolean> getUseVenv()

    /**
     * Automatically install virtualenv (if pip modules used)
     */
    @Input
    abstract Property<Boolean> getInstallVirtualenv()

    /**
     * Virtualenv version to install. Used only if no virtualenv already installed
     */
    @Input
    abstract Property<String> getVirtualenvVersion()

    /**
     * Minimal virtualenv version to work with
     */
    @Input
    @Optional
    abstract Property<String> getMinVirtualenvVersion()

    /**
     * Copy virtual environment instaead of symlink
     */
    @Input
    abstract Property<Boolean> getEnvCopy()

    @TaskAction
    void run() {
        // use extension value to initialize service default (it is the only place where it could be done)
        envService.get().setPythonPath(gradleEnv.get().projectPath, pythonPath.orNull)

        boolean envRequested = scope.get() >= PythonExtension.Scope.VIRTUALENV_OR_USER
        EnvSupport env = envRequested
                ? (useVenv.get() ? new VenvSupport(this) : new VirtualenvSupport(this)) : null

        // preventing simultaneous installation of virtualenv by multiple modules when parallel execution enabled
        synchronized (SYNC) {
            // use env right ahead (global python could even not exists), but only if allowed by scope
            if (envRequested && env.exists()) {
                virtual = true
            } else {
                // normal flow: check global installation - try to create virtualenv (if modules required)

                checkPython()

                if (modulesInstallationRequired) {
                    checkPip()
                    // only if virtualenv usage requested
                    if (envRequested) {
                        try {
                            virtual = checkEnv(env)
                        } catch (FallbackException ignored) {
                            // note: weak point of fallback is that .exists() method was called before this point on
                            // venv support object, but check logic is the same for both so no harm. Also, note that
                            // venv existence not checked immediately after creation to avoid redundant python call
                            // in cases when environment already exists
                            env = new VirtualenvSupport(this)
                            virtual = checkEnv(env)
                        }
                    }
                }
            }
        }

        if (virtual) {
            switchEnvironment(env)
        }

        if (!python.canonicalBinaryDir.startsWith(python.canonicalHomeDir)) {
            logger.error("WARNING: Python binary path '{}' does not match home path reported by python (sys.prefix): " +
                    "'{}'. Everything could still work as expected if code doesn't rely on python location.",
                    python.canonicalBinaryDir, python.canonicalHomeDir
            )
        }
    }

    @Internal
    String getActualPythonPath() {
        return envService.get().getPythonPath(gradleEnv.get().projectPath)
    }

    private void checkPython() {
        // important because python could change on second execution
        Python python = new Python(gradleEnv.get(), actualPythonPath, pythonBinary.orNull)
                .workDir(workDir.orNull)
                .environment(environment.get())
                .validateSystemBinary(validateSystemBinary.get())
                .withDocker(docker.toConfig())
                .validate()
        try {
            python.version
        } catch (ExecException ex) {
            throw new GradleException("Python not found: $python.canonicalUsedBinary. " + (virtual ?
                    'This must be a bug of virtualenv support, please report it ' +
                            '(https://github.com/xvik/gradle-use-python-plugin/issues). You can disable ' +
                            'virtualenv usage with \'python.scope = USER\'.'
                    : 'Please install it (http://docs.python-guide.org/en/latest/starting/installation/) ' +
                    'or configure correct location with \'python.pythonPath\'.'), ex)
        }
        checkPythonVersion(python)
    }

    private void checkPythonVersion(Python python) {
        String version = python.version
        if (!CliUtils.isVersionMatch(version, minPythonVersion.orNull)) {
            throw new GradleException("Python ($python.homeDir) verion $version does not match minimal " +
                    "required version: ${minPythonVersion.get()}")
        }
        logger.lifecycle('Using python {} from {} ({})',
                python.version, python.canonicalHomeDir, python.canonicalUsedBinary)
    }

    private void checkPip() {
        // important because python could change on second execution
        Pip pip = new Pip(gradleEnv.get(), actualPythonPath, pythonBinary.orNull)
                .userScope(false)
                .workDir(workDir.orNull)
                .environment(environment.get())
                .validateSystemBinary(validateSystemBinary.get())
                .withDocker(getDocker().toConfig())
                .validate()
        try {
            pip.versionLine
        } catch (PythonExecutionFailed ex) {
            throw new GradleException("Pip is not installed${virtual ? " on virtualenv ${envPath.get()}" : ''}. " +
                    'Please install it (https://pip.pypa.io/en/stable/installing/).', ex)
        }
        checkPipVersion(pip)
    }

    private void checkPipVersion(Pip pip) {
        String version = pip.version
        if (!CliUtils.isVersionMatch(version, minPipVersion.orNull)) {
            throw new GradleException("Pip verion $version does not match minimal " +
                    "required version: ${minPipVersion.get()}. Use 'pip install -U pip' to upgrade pip.")
        }
        logger.lifecycle('Using {}', pip.versionLine)
    }

    // not static only because it tries to cast venv support to virtualenv support class (why??? looks like a bug)
    @CompileStatic(TypeCheckingMode.SKIP)
    private boolean checkEnv(EnvSupport env) {
        Pip pip = new Pip(gradleEnv.get(), actualPythonPath, pythonBinary.orNull)
                .userScope(true)
                .breakSystemPackages(breakSystemPackages.get())
                .workDir(workDir.orNull)
                .environment(environment.get())
                .validateSystemBinary(validateSystemBinary.get())
                .withDocker(getDocker().toConfig())
                .validate()

        gradleEnv.get().debug('Creating environment')
        envCreated = env.create(pip)
        return envCreated
    }

    private void switchEnvironment(EnvSupport env) {
        // switch environment and check again
        envService.get().setPythonPath(gradleEnv.get().projectPath, env.pythonPath)
        // note: after changing pythonPath, configured pythonBinary would be actually ignored and so no need to change

        checkPython()
        // only if pip required or requirements file present
        if (modulesInstallationRequired) {
            checkPip()
        }

        if (!python.virtualenv) {
            throw new GradleException('Configured environment is not a virtualenv: ' +
                    "${gradleEnv.get().file(envPath.get()).absolutePath}. " +
                    'Most likely, issue appear due to incorrect `python.envPath` configuration.')
        }

        if (envCreated) {
            // own files, created within docker (unroot)
            // executed not after creation because python will create new files on first run
            dockerChown(envPath.get())
        }
    }
}
