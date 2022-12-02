package ru.vyarus.gradle.plugin.python.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 17.03.2020
 */
class PythonTaskEnvironmentKitTest extends AbstractKitTest {

    def "Check env vars"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            tasks.register('sample', PythonTask) {
                command = "-c \\"import os;print('variables: '+os.getenv('some', 'null')+' '+os.getenv('foo', 'null'))\\""
                environment 'some', 1
                environment(['foo': 'bar'])
            }
        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "variables visible"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('variables: 1 bar')
    }


    def "Check python see system variables"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            assert System.getenv('some') == 'foo'
            
            tasks.register('sample', PythonTask) {
                command = "-c \\"import os;print('variables: '+os.getenv('some', 'null'))\\""
            }
        """

        when: "run task"
        def env = new HashMap(System.getenv())
        env.put('some', 'foo')
        BuildResult result = gradle('sample')
                .withEnvironment(env)
                .build()

        then: "system variable visible"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('variables: foo')
    }

    def "Check python dont see system variables after override"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            assert System.getenv('some') == 'foo'
            
            tasks.register('sample', PythonTask) {
                command = "-c \\"import os;print('variables: '+os.getenv('some', 'null'))\\""
                environment 'bar', 1
            }
        """

        when: "run task"
        def env = new HashMap(System.getenv())
        env.put('some', 'foo')
        BuildResult result = gradle('sample')
                .withEnvironment(env)
                .build()

        then: "setting variable doesn't hide system vars"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('variables: foo')
    }

    def "Check composition with blobal vars"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.environment 'some', 1

            tasks.register('sample', PythonTask) {
                command = "-c \\"import os;print('variables: '+os.getenv('some', 'null')+' '+os.getenv('foo', 'null'))\\""
                environment 'foo', 'bar'
            }
        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "both variables visible"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('variables: 1 bar')
    }
}
