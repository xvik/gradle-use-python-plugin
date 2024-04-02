package ru.vyarus.gradle.plugin.python.task.env

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import ru.vyarus.gradle.plugin.python.PythonExtension
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv
import ru.vyarus.gradle.plugin.python.task.CheckPythonTask
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * Virtualenv support implementation.
 *
 * @author Vyacheslav Rusakov
 * @since 01.04.2024
 */
@CompileStatic
class VirtualenvSupport implements EnvSupport {
    private static final String PROP_VENV_INSTALLED = 'virtualenv.installed'

    private final CheckPythonTask task
    private final Virtualenv env

    VirtualenvSupport(CheckPythonTask task) {
        this.task = task
        env = new Virtualenv(task.gradleEnv.get(), task.pythonPath.orNull, task.pythonBinary.orNull,
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
            venvInstalled = pip.isInstalled(env.name)
            task.gradleEnv.get().updateGlobalCache(PROP_VENV_INSTALLED, venvInstalled)
        }
        if (!venvInstalled) {
            if (task.installVirtualenv.get()) {
                // automatically install virtualenv if allowed (in --user)
                // by default, exact (configured) version used to avoid side effects!)
                pip.install(env.name + (task.virtualenvVersion.orNull ? "==${task.virtualenvVersion.get()}" : ''))
                task.gradleEnv.get().updateGlobalCache(PROP_VENV_INSTALLED, true)
            } else if (task.scope.get() == PythonExtension.Scope.VIRTUALENV) {
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
            task.logger.error('WARNING: Global python is already a virtualenv: \'{}\'. New environment would be ' +
                    'created based on it: \'{}\'. In most cases, everything would work as expected.',
                    pip.python.binaryDir, task.envPath.get())
        }

        task.logger.lifecycle("Using $env.versionLine (in '${task.envPath.get()}')")

        if (!CliUtils.isVersionMatch(env.version, task.minVirtualenvVersion.orNull)) {
            throw new GradleException("Installed virtualenv version $env.version does not match minimal " +
                    "required version ${task.minVirtualenvVersion.get()}. \nVirtualenv " +
                    "${task.minVirtualenvVersion.get()} is recommended but older version could also be used. " +
                    '\nEither configure lower minimal required ' +
                    "version with [python.minVirtualenvVersion=\'$env.version\'] \nor upgrade installed " +
                    "virtualenv with [pip install -U virtualenv==${task.virtualenvVersion.get()}] \n(or just remove " +
                    'virtualenv with [pip uninstall virtualenv] and plugin will install the correct version itself)')
        }

        // symlink by default (copy if requested by user config)
        env.create(task.envCopy.get())
        return true
    }

    @Override
    String getPythonPath() {
        return env.pythonPath
    }
}
