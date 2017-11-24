package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
class PythonPluginKitTest extends AbstractKitTest {

    def "Check plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
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

    def "Check python version requirement"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                minVersion = '5'
            }

        """

        when: "run task"
        BuildResult result = runFailed('pipInstall')

        then: "task successful"
        result.output.contains('does not match minimal required version: 5')
    }
}