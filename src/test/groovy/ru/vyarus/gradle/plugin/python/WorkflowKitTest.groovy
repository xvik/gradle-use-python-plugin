package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Venv
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv

/**
 * @author Vyacheslav Rusakov
 * @since 15.12.2017
 */
class WorkflowKitTest extends AbstractKitTest {

    def "Check no pip modules - no env created"() {

        setup:
        Virtualenv env = env()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "no env created (global binary used)"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output =~ / \(python(3)?\)/
        !env.exists()
    }

    def "Check no pip modules - existing env used"() {

        setup: "create env with default path so it could be detected"
        Virtualenv env = env()
        env.createPythonOnly()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "existing env used"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains(isWin ? env.pythonPath.replace('/', '\\') : env.pythonPath)
    }

    def "Check restricted user scope - existing env not used"() {

        setup: "create recognizable env but force user scope"
        Virtualenv env = env()
        env.createPythonOnly()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "existing env not used"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output =~ / \(python(3)?\)/
    }

    def "Check use existing env"() {

        setup: "create env inti not default pat and reconfigure plugin to recognize it"
        Virtualenv env = env('env')
        env.createPythonOnly()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.envPath = 'env'
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "existing env used"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains(isWin ? env.pythonPath.replace('/', '\\') : env.pythonPath)
    }

    def "Check prevent virtualenv installation"() {

        setup: "create undetectable virtualenv and use it as pure python"
        Virtualenv env = env('env')
        env.create()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                   
            python.pip 'extract-msg:0.28.0'     
            python.pythonPath = '${env.pythonPath.replace("\\", "\\\\")}'
            python.installVirtualenv = false
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "virtualenv not installed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        !result.output.contains('-m pip install virtualenv')
    }

    def "Check copy env"() {

        setup:
        Virtualenv env = env()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                     
            python.envCopy = true
            python.useVenv = false
            python.pip 'extract-msg:0.28.0'
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "env created, but not symlinked"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('--always-copy')
        env.exists()
    }

    def "Check copy venv"() {

        setup:
        Venv env = venv()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                     
            python.envCopy = true
            python.useVenv = true
            python.pip 'extract-msg:0.28.0'
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "env created, but not symlinked"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('--copies')
        env.exists()
    }

    def "Check pip not installed"() {

        setup: "use custom env without pip as pure python"
        Virtualenv env = env('env')
        env.createPythonOnly()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                        
            python.pythonPath = '${env.pythonPath.replace("\\", "\\\\")}'
            python.useVenv = false            
            python.pip 'extract-msg:0.28.0'
        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "error - pip not installed"
        result.output.contains('Pip is not installed')
    }

    def "Check environment without pip"() {

        setup: "detectable environment did not contain pip"
        Virtualenv env = env()
        env.createPythonOnly()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                        
            python.pip 'extract-msg:0.28.0'
        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "error - pip not installed"
        result.output.contains('Pip is not installed on virtualenv')
    }

    def "Check virtualenv required"() {

        setup: "virtualenv used as usual python (path not match configured envPath)"
        Virtualenv env = env('env')
        env.create()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                        
            python {
                pythonPath = '${env.pythonPath.replace("\\", "\\\\")}'
                installVirtualenv = false
                scope = VIRTUALENV             
                pip 'extract-msg:0.28.0'
                useVenv = false
            }
        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "virtualenv not found"
        result.output.contains('Virtualenv is not installed')
    }

    def "Check global python binary validation"() {

        setup: "detectable environment"
        Virtualenv env = env()
        env.createPythonOnly()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                        
            python {
                pythonBinary = 'pyt'
                validateSystemBinary = true
            }
        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "error - python not found"
        result.output.contains('\'pyt\' executable was not found in system. Please check PATH variable correctness (current process may not see the same PATH as your shell).')
    }

    def "Check fail to detect python binary in environment"() {

        setup: "detectable environment"
        Virtualenv env = env()
        env.createPythonOnly()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
                        
            python {
                pythonBinary = 'pyt'
                validateSystemBinary = false
            }
        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "error - python not found"
        result.output.contains('This must be a bug of virtualenv support, please report it')
    }
}
