package ru.vyarus.gradle.plugin.python.task.env

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Venv
import ru.vyarus.gradle.plugin.python.task.CheckPythonTask

/**
 * Venv support implementation.
 *
 * @author Vyacheslav Rusakov
 * @since 01.04.2024
 */
@CompileStatic
class VenvSupport implements EnvSupport {
    private static final String PROP_VENV_INSTALLED = 'venv.installed'

    private final CheckPythonTask task
    private final Venv env

    VenvSupport(CheckPythonTask task) {
        this.task = task
        env = new Venv(task.gradleEnv.get(), task.pythonPath.orNull, task.pythonBinary.orNull,
                task.envPath.orNull)
                .validateSystemBinary(task.validateSystemBinary.get())
                .withDocker(task.docker.toConfig())
                .workDir(task.workDir.orNull)
                .environment(task.environment.get())
                .validate()
    }

    @Override
    boolean exists() {
        return env.exists()
    }

    @Override
    boolean create(Pip pip) {
        // to avoid calling pip in EACH module (in multi-module project) to verify virtualenv existence
        Boolean venvInstalled = task.gradleEnv.get().globalCache(PROP_VENV_INSTALLED, null)
        if (venvInstalled == null) {
            venvInstalled = env.installed
            task.gradleEnv.get().updateGlobalCache(PROP_VENV_INSTALLED, venvInstalled)
        }
        if (!venvInstalled) {
            task.logger.warn('WARNING: Venv python module is not found, fallback to virtualenv')
            // fallback to virtualenv (no attempt to install it as it could be managed by system package)
            throw new FallbackException()
        }

        if (pip.python.virtualenv) {
            task.logger.error('WARNING: Global python is already a virtualenv: \'{}\'. New environment would be ' +
                    'created based on it: \'{}\'. In most cases, everything would work as expected.',
                    pip.python.binaryDir, task.envPath.get())
        }

        // no version for venv as its synchronized with python
        task.logger.lifecycle("Using venv (in '${task.envPath.get()}')")

        // symlink by default (copy if requested by user config)
        env.create(task.envCopy.get())
        return true
    }

    @Override
    String getPythonPath() {
        return env.pythonPath
    }
}
