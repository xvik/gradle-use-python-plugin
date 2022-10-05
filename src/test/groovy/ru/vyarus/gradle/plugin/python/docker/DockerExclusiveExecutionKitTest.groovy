package ru.vyarus.gradle.plugin.python.docker

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 04.10.2022
 */
class DockerExclusiveExecutionKitTest extends AbstractKitTest {

    def "Check exclusive execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker { use = true }
            }
            
            task sample(type: PythonTask) {
                docker.exclusive = true
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] executing command in exclusive container')
        result.output.contains('samplee')
    }

    def "Check exclusive execution with closure syntax"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker { use = true }
            }
            
            task sample(type: PythonTask) {
                docker {
                   exclusive = true
                }  
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] executing command in exclusive container')
        result.output.contains('samplee')
    }

    def "Check exclusive fail"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {                               
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                docker.exclusive = true
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
