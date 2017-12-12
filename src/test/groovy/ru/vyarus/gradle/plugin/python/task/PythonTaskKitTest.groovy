package ru.vyarus.gradle.plugin.python.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PythonTaskKitTest extends AbstractKitTest {

    def "Check work dir"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            task sample(type: PythonTask) {
                workDir = 'build/pyth'
                command = '-c "open(\\'fl\\', \\'a\\').close()"'
            }
        """

        when: "run task"
        BuildResult result = run('sample')

        then: "file created in work dir"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        file('build/pyth').list({fl, name-> name == 'fl'} as FilenameFilter).size() == 1
    }

    def "Check no work dir creation"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            task sample(type: PythonTask) {
                workDir = 'build/pyth'
                createWorkDir = false
                command = '-c "open(\\'fl\\', \\'a\\').close()"'
            }
        """

        when: "run task"
        BuildResult result = runFailed('sample')

        then: "python failed to start"
        result.output =~ /net\.rubygrapefruit\.platform\.NativeException: Could not start 'python(3)?'/
    }

    def "Check module command"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            task sample(type: PythonTask) {
                module = 'pip'
                command = 'list --user'
            }
        """

        when: "run task"
        BuildResult result = run('sample')

        then: "executed"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python] python(3)? -m pip list --user/
    }

    def "Check log level change"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            task sample(type: PythonTask) {
                module = 'pip'
                command = 'list'
                logLevel = LogLevel.DEBUG
            }
        """

        when: "run task"
        BuildResult result = run('sample')

        then: "executed"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        !result.output.contains('[python] python -m pip list')
    }

    def "Check different prefix"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            task sample(type: PythonTask) {
                module = 'pip'
                command = 'list'
                outputPrefix = '---->'
            }
        """

        when: "run task"
        BuildResult result = run('sample')

        then: "executed"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('---->')
    }

    def "Check extra args"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            task sample(type: PythonTask) {
                module = 'pip'
                command = 'list'
                extraArgs '--format=columns', '--user'
            }
        """

        when: "run task"
        BuildResult result = run('sample')

        then: "executed"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python] python(3)? -m pip list --format=columns --user/
    }

    def "Check script file call"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            task script(type: PythonTask) {
                command = 'sample.py'
            }
        """
        file('sample.py') << "print('sample')"

        when: "run task"
        BuildResult result = run('script')

        then: "executed"
        result.task(':script').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python] python(3)? sample.py/
        result.output.contains('\t sample')
    }

}
