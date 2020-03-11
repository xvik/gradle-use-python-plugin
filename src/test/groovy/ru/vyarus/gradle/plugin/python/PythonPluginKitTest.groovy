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
                pip 'click:6.7'
            }
            
            task sample(type: PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /click\s+6.7/
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
                pip 'click:6.7'
            }
            
            task sample(type: PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /click\s+6.7/
        result.output.contains('samplee')
    }

    def "Check module override"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'click:6.7', 'click:6.6'
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /click\s+6.6/
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
                pip 'click:6.7'
                pythonPath = "${env.pythonPath.replace('\\', '\\\\')}"
                virtualenvVersion = "16.7.5"
            }            
        """

        when: "run task"
        BuildResult result = run('checkPython')

        then: "task successful"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m pip install virtualenv==16.7.5')
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
                pip 'click:6.7'
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
}