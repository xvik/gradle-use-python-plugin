package ru.vyarus.gradle.plugin.python.docker

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 04.10.2022
 */
// testcontainers doesn't work on windows server https://github.com/testcontainers/testcontainers-java/issues/2960
@IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
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
            
            tasks.register('sample', PythonTask) {
                docker.exclusive = true
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] exclusive container')
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
            
            tasks.register('sample', PythonTask) {
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
        result.output.contains('[docker] exclusive container')
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
            
            tasks.register('sample', PythonTask) {
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
