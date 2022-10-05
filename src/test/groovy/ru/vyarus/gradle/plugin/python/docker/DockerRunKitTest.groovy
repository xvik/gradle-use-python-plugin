package ru.vyarus.gradle.plugin.python.docker

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 21.09.2022
 */
class DockerRunKitTest extends AbstractKitTest {

    def "Check simple execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output.contains('samplee')
    }


    def "Check pip-powered execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'extract-msg:0.28.0'
                
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }

    def "Check user-scoped execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'extract-msg:0.28.0'
                scope=USER
                
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }


    def "Check in-container fail"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                command = '-c printTt(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = runFailed('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.FAILED
        result.output.contains('\'printTt\' is not defined')
    }
}
