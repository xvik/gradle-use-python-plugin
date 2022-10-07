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
@IgnoreIf({ env.containsKey('APPVEYOR') })
class DockerAutoRestartKitTest extends AbstractKitTest {

    def "Check auto container restart due to changed working dir"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                workDir = 'build'
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('Restarting container due to changed working directory')
        result.output.contains('samplee')
    }

    def "Check auto container restart due to changed environment config"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                environment 'ONE', 'one'                            
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                environment 'ONE', 'two'
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('Restarting container due to changed environment variables')
        result.output.contains('samplee')
    }

    def "Check no auto container restart due to same environment config"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                environment 'ONE', 'one'                            
                docker.use = true
            }
            
            task sample(type: PythonTask) {
                environment 'ONE', 'one'
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        !result.output.contains('Restarting container due to changed')
        result.output.contains('samplee')
    }
}
