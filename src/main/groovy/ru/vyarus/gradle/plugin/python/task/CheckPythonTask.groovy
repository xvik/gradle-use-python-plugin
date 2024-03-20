package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecException
import ru.vyarus.gradle.plugin.python.PythonExtension
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Python
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv
import ru.vyarus.gradle.plugin.python.service.EnvService
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
class CheckPythonTask extends BasePipTask {

    private static final String PROP_VENV_INSTALLED = 'virtualenv.installed'
    private static final Object SYNC = new Object()
    private boolean virtual = false
    private boolean envCreated = false

    /**
     * Virtualenv scope
     */
    @Input
    PythonExtension.Scope scope

    /**
     * Virtualenv folder path
     */
    @Input
    String envPath

    /**
     * Minimal allowed python version
     */
    @Input
    @Optional
    String minPythonVersion

    /**
     * Minimal allowed pip version
     */
    @Input
    @Optional
    String minPipVersion

    /**
     * Automatically install virtualenv (if pip modules used)
     */
    @Input
    boolean installVirtualenv

    /**
     * Virtualenv version to install. Used only if no virtualenv already installed
     */
    @Input
    String virtualenvVersion

    /**
     * Minimal virtualenv version to work with
     */
    @Input
    @Optional
    String minVirtualenvVersion

    /**
     * Copy virtual environment instaead of symlink
     */
    @Input
    boolean envCopy

    // service holds actual pythonPath. All tasks would use it for lazy default, but check task could CHANGE it
    // (that's why it's important to call it before all other tasks)
    @Internal
    Provider<EnvService> envService

    @TaskAction
    @SuppressWarnings('UnnecessaryGetter')
    void run() {
        boolean envRequested = getScope() >= PythonExtension.Scope.VIRTUALENV_OR_USER
        Virtualenv env = envRequested
                // synchronize work dir between python instances
                ? new Virtualenv(gradleEnv, getPythonPath(), getPythonBinary(), getEnvPath())
                .validateSystemBinary(getValidateSystemBinary())
                .withDocker(getDocker().toConfig())
                .workDir(getWorkDir())
                .environment(getEnvironment())
                .validate() : null

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
                        virtual = checkEnv(env)
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

    private void checkPython() {
        // important because python could change on second execution
        Python python = new Python(gradleEnv, pythonPath, pythonBinary)
                .workDir(getWorkDir())
                .environment(getEnvironment())
                .validateSystemBinary(isValidateSystemBinary())
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
        if (!CliUtils.isVersionMatch(version, getMinPythonVersion())) {
            throw new GradleException("Python ($python.homeDir) verion $version does not match minimal " +
                    "required version: ${getMinPythonVersion()}")
        }
        logger.lifecycle('Using python {} from {} ({})',
                python.version, python.canonicalHomeDir, python.canonicalUsedBinary)
    }

    private void checkPip() {
        // important because python could change on second execution
        Pip pip = new Pip(gradleEnv, getPythonPath(), getPythonBinary())
                .userScope(false)
                .workDir(getWorkDir())
                .environment(getEnvironment())
                .validateSystemBinary(isValidateSystemBinary())
                .withDocker(getDocker().toConfig())
                .validate()
        try {
            pip.versionLine
        } catch (PythonExecutionFailed ex) {
            throw new GradleException("Pip is not installed${virtual ? " on virtualenv $envPath" : ''}. " +
                    'Please install it (https://pip.pypa.io/en/stable/installing/).', ex)
        }
        checkPipVersion(pip)
    }

    private void checkPipVersion(Pip pip) {
        String version = pip.version
        if (!CliUtils.isVersionMatch(version, getMinPipVersion())) {
            throw new GradleException("Pip verion $version does not match minimal " +
                    "required version: ${getMinPipVersion()}. Use 'pip install -U pip' to upgrade pip.")
        }
        logger.lifecycle('Using {}', pip.versionLine)
    }

    @SuppressWarnings('MethodSize')
    private boolean checkEnv(Virtualenv env) {
        Pip pip = new Pip(gradleEnv, getPythonPath(), getPythonBinary())
                .userScope(true)
                .workDir(getWorkDir())
                .environment(getEnvironment())
                .validateSystemBinary(isValidateSystemBinary())
                .withDocker(getDocker().toConfig())
                .validate()
        // to avoid calling pip in EACH module (in multi-module project) to verify virtualenv existence
        Boolean venvInstalled = gradleEnv.globalCache(PROP_VENV_INSTALLED, null)
        if (venvInstalled == null) {
            venvInstalled = pip.isInstalled(env.name)
            gradleEnv.updateGlobalCache(PROP_VENV_INSTALLED, venvInstalled)
        }
        if (!venvInstalled) {
            if (isInstallVirtualenv()) {
                // automatically install virtualenv if allowed (in --user)
                // by default, exact (configured) version used to avoid side effects!)
                pip.install(env.name + (getVirtualenvVersion() ? "==${getVirtualenvVersion()}" : ''))
                gradleEnv.updateGlobalCache(PROP_VENV_INSTALLED, true)
            } else if (getScope() == PythonExtension.Scope.VIRTUALENV) {
                // virtualenv strictly required - fail
                throw new GradleException('Virtualenv is not installed. Please install it ' +
                        '(https://virtualenv.pypa.io/en/stable/installation/) or change target pip ' +
                        "scope 'python.scope' from ${PythonExtension.Scope.VIRTUALENV}")
            } else {
                // not found, but ok (fallback to USER scope)
                return false
            }
        }

        if (pip.python.virtualenv) {
            logger.error('WARNING: Global python is already a virtualenv: \'{}\'. New environment would be ' +
                    'created based on it: \'{}\'. In most cases, everything would work as expected.',
                    pip.python.binaryDir, getEnvPath())
        }

        logger.lifecycle("Using $env.versionLine (in '${getEnvPath()}')")

        if (!CliUtils.isVersionMatch(env.version, getMinVirtualenvVersion())) {
            throw new GradleException("Installed virtualenv version $env.version does not match minimal " +
                    "required version ${getMinVirtualenvVersion()}. \nVirtualenv ${getMinVirtualenvVersion()} is " +
                    'recommended but older version could also be used. \nEither configure lower minimal required ' +
                    "version with [python.minVirtualenvVersion=\'$env.version\'] \nor upgrade installed " +
                    "virtualenv with [pip install -U virtualenv==${getVirtualenvVersion()}] \n(or just remove " +
                    'virtualenv with [pip uninstall virtualenv] and plugin will install the correct version itself)')
        }

        // symlink by default (copy if requested by user config)
        env.create(isEnvCopy())
        envCreated = true
        return true
    }

    @SuppressWarnings('UnnecessaryGetter')
    private void switchEnvironment(Virtualenv env) {
        // switch environment and check again
        envService.get().setPythonPath(gradleEnv.projectPath, env.pythonPath)
        this.pythonPath = env.pythonPath

        checkPython()
        // only if pip required or requirements file present
        if (modulesInstallationRequired) {
            checkPip()
        }

        if (!python.isVirtualenv()) {
            throw new GradleException("Configured environment is not a virtualenv: ${env.location.absolutePath}. " +
                    'Most likely, issue appear due to incorrect `python.envPath` configuration.')
        }

        if (envCreated) {
            // own files, created within docker (unroot)
            // executed not after creation because python will create new files on first run
            dockerChown(env.path)
        }
    }
}
