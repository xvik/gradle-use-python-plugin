package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecException
import ru.vyarus.gradle.plugin.python.PythonExtension
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Python
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv
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
 *
 * @author Vyacheslav Rusakov
 * @since 08.12.2017
 */
@CompileStatic
class CheckPythonTask extends BasePipTask {

    private boolean virtual = false

    @TaskAction
    @SuppressWarnings('UnnecessaryGetter')
    void run() {
        PythonExtension ext = project.extensions.findByType(PythonExtension)
        boolean envRequested = ext.scope >= PythonExtension.Scope.VIRTUALENV_OR_USER
        Virtualenv env = envRequested ? new Virtualenv(project, ext.pythonPath, ext.pythonBinary, ext.envPath) : null

        // use env right ahead (global python could even not exists), but only if allowed by scope
        if (envRequested && env.exists()) {
            virtual = true
        } else {
            // normal flow: check global installation - try to create virtualenv (if modules required)

            checkPython(ext)

            if (!getModules().empty) {
                checkPip(ext)
                // only if virtualenv usage requested
                if (envRequested) {
                    virtual = checkEnv(env, ext)
                }
            }
        }

        if (virtual) {
            switchEnvironment(env, ext)
        }

        if (!python.binaryDir.startsWith(python.homeDir)) {
            logger.error("WARNING: Python binary path '{}' does not match home path reported by python (sys.prefix): " +
                    "'{}'. Everything could still work as expected if code doesn't rely on python location.",
                    python.binaryDir, python.homeDir
            )
        }
    }

    private void checkPython(PythonExtension ext) {
        // important because python could change on second execution
        Python python = new Python(project, pythonPath, pythonBinary)
        try {
            python.version
        } catch (ExecException ex) {
            throw new GradleException("Python not found: $python.usedBinary. " + (virtual ?
                    'This must be a bug of virtualenv support, please report it ' +
                            '(https://github.com/xvik/gradle-use-python-plugin/issues). You can disable ' +
                            'virtualenv usage with \'python.scope = USER\'.'
                    : 'Please install it (http://docs.python-guide.org/en/latest/starting/installation/) ' +
                    'or configure correct location with \'python.pythonPath\'.'), ex)
        }
        checkPythonVersion(python, ext)
    }

    private void checkPythonVersion(Python python, PythonExtension ext) {
        String version = python.version
        String minVersion = ext.minPythonVersion
        if (!CliUtils.isVersionMatch(version, minVersion)) {
            throw new GradleException("Python ($python.homeDir) verion $version does not match minimal " +
                    "required version: $minVersion")
        }
        logger.lifecycle('Using python {} from {} ({})', python.version, python.homeDir, python.usedBinary)
    }

    private void checkPip(PythonExtension ext) {
        // important because python could change on second execution
        Pip pip = new Pip(project, ext.pythonPath, ext.pythonBinary, false)
        try {
            pip.versionLine
        } catch (PythonExecutionFailed ex) {
            throw new GradleException("Pip is not installed${virtual ? " on virtualenv $ext.envPath" : ''}. " +
                    'Please install it (https://pip.pypa.io/en/stable/installing/).', ex)
        }
        checkPipVersion(pip, ext)
    }

    private void checkPipVersion(Pip pip, PythonExtension ext) {
        String version = pip.version
        String minVersion = ext.minPipVersion
        if (!CliUtils.isVersionMatch(version, minVersion)) {
            throw new GradleException("Pip verion $version does not match minimal " +
                    "required version: $minVersion. Use 'pip install -U pip' to upgrade pip.")
        }
        logger.lifecycle('Using {}', pip.versionLine)
    }

    private boolean checkEnv(Virtualenv env, PythonExtension ext) {
        Pip pip = new Pip(project, ext.pythonPath, ext.pythonBinary, true)
        if (!pip.isInstalled(env.name)) {
            if (ext.installVirtualenv) {
                // automatically install virtualenv if allowed (in --user)
                pip.install(env.name)
            } else if (ext.scope == PythonExtension.Scope.VIRTUALENV) {
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
                    pip.python.binaryDir, ext.envPath)
        }

        logger.lifecycle("Using virtualenv $env.version ($ext.envPath)")
        // symlink by default (copy if requested by user config)
        env.create(ext.envCopy)
        return true
    }

    @SuppressWarnings('UnnecessaryGetter')
    private void switchEnvironment(Virtualenv env, PythonExtension ext) {
        // switch environment and check again
        ext.pythonPath = env.pythonPath
        this.pythonPath = ext.pythonPath

        checkPython(ext)
        // only if pip required
        if (!getModules().empty) {
            checkPip(ext)
        }

        if (!python.isVirtualenv()) {
            throw new GradleException("Convfigured environment is not a virtualenv: ${env.location.absolutePath}. " +
                    'Most likely, issue appear due to incorrect `python.envPath` configuration.')
        }
    }
}
