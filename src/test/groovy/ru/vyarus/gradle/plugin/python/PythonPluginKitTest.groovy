package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv

/**
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
class PythonPluginKitTest extends AbstractKitTest {

    def "Check simple plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'extract-msg:0.28.0'
            }
            
            task sample(type: PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }

    def "Check env plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
            }
            
            task sample(type: PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }

    def "Check module override"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'extract-msg:0.28.1', 'extract-msg:0.28.0'
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
    }

    def "Check exact virtualenv version installation"() {
        setup:
        Virtualenv env = env('env')
        env.create()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
                pythonPath = "${env.pythonPath.replace('\\', '\\\\')}"
                virtualenvVersion = "20.4.0"
            }            
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "task successful"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m pip install virtualenv==20.4.0')
    }

    def "Check install the latest virtualenv case"() {
        setup:
        Virtualenv env = env('env')
        env.create()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
                pythonPath = "${env.pythonPath.replace('\\', '\\\\')}"
                virtualenvVersion = ""
            }            
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "task successful"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m pip install virtualenv')
        !result.output.contains('-m pip install virtualenv=')
    }

    def "Check min virtualenv verification"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {                
                minVirtualenvVersion '1000'
                pip 'extract-msg:0.28.1'
            }            
        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "virtualenv version error"
        result.output.contains('does not match minimal required version 1000')
    }

    def "Check env cleanup"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {                
                pip 'extract-msg:0.28.1'
            }            
        """

        when: "create env"
        BuildResult result = run('checkPython')

        then: "created"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        file('.gradle/python').exists()

        when: "cleanup env"
        result = run('cleanPython')

        then: "cleared"
        result.task(':cleanPython').outcome == TaskOutcome.SUCCESS
        !file('.gradle/python').exists()

        when: "create env again"
        result = run('checkPython')

        then: "created"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        file('.gradle/python').exists()
    }

    def "Check env cleanup disabled when virtualenv not used"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {                
                scope = ru.vyarus.gradle.plugin.python.PythonExtension.Scope.USER
            }            
        """

        when: "clean"
        BuildResult result = run('cleanPython')

        then: "ignored"
        result.task(':cleanPython').outcome == TaskOutcome.SKIPPED
    }
}