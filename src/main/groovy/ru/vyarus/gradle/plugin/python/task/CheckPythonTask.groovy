package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.taskdefs.condition.Os
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
 * Task validates python installation (will fail if python or pip not found or minimal version doesn't match).
 * Task called before any {@link ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask}
 * (by default, before pipInstall).
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
        checkPython(ext)

        if (!getModules().empty) {

            checkPip(ext)
            if (ext.scope >= PythonExtension.Scope.VIRTUALENV_OR_USER) {
                virtual = checkEnv(ext)
            }
        }

        if (virtual) {
            boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
            // switch environment and check again
            ext.pythonPath = isWindows ? "$ext.envPath/Scripts" : "$ext.envPath/bin"
            checkPython(ext)
            checkPip(ext)
        }
        alterPipTasks()
    }

    private void checkPython(PythonExtension ext) {
        // important because python could change on second execution
        Python python = new Python(project, pythonPath, pythonBinary)
        try {
            python.exec('--version')
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

    private boolean checkEnv(PythonExtension ext) {
        Virtualenv env = new Virtualenv(project, ext.pythonPath, ext.pythonBinary, ext.envPath)
        try {
            env.version
        } catch (PythonExecutionFailed ex) {
            if (ext.scope == PythonExtension.Scope.VIRTUALENV) {
                throw new GradleException('Virtualenv is not installed. Please install it ' +
                        '(https://virtualenv.pypa.io/en/stable/installation/) or change target pip ' +
                        "scope 'python.scope' from ${PythonExtension.Scope.VIRTUALENV}", ex)
            }
            // not found, but ok (fallback to USER scope)
            return false
        }

        logger.lifecycle("Using virtualenv $env.version ($ext.envPath)")
        env.create()
        return true
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void alterPipTasks() {
        if (virtual) {
            // disable user scope (not allowed in virtualenv)
            project.tasks.withType(BasePipTask) { task ->
                task.userScope = false
            }
        }
    }
}
