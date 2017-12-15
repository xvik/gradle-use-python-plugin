package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv

/**
 * @author Vyacheslav Rusakov
 * @since 15.12.2017
 */
class WorkflowKitTest extends AbstractKitTest {

    def "No pip modules - no env used"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
        """
        Virtualenv env = env()

        when: "run task"
        BuildResult result = run('checkPython')

        then: "no env created"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        !result.output.contains('python -m virtualenv .gradle/python')
        !env.exists()
    }
}
