package ru.vyarus.gradle.plugin.python.task


import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Python

/**
 * @author Vyacheslav Rusakov
 * @since 01.12.2017
 */
class PipUpdatesTaskKitTest extends AbstractKitTest {

    def "Check updates detected"() {

        setup:
        // make sure old version installed
        new Pip(gradleEnv()).install('extract-msg==0.28.0')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'extract-msg:0.28.0'
            }

        """

        when: "run task"
        BuildResult result = run('pipUpdates')

        then: "extract-msg update detected"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('The following modules could be updated:')
        result.output =~ /extract-msg\s+0.28.0/
    }

    def "Check updates detected in environment"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
            }

        """

        when: "install old version"
        BuildResult result = run('pipInstall')
        then: "installed"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip install extract-msg')


        when: "run task"
        result = run('pipUpdates')

        then: "extract-msg update detected"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('The following modules could be updated:')
        result.output =~ /extract-msg\s+0.28.0/
    }

    def "Check no modules"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
        """

        when: "run task"
        BuildResult result = run('pipUpdates')

        then: "nothing declared"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('No modules declared')
    }

    def "Check no updates detected"() {

        setup:
        // use the latest version
        new Python(gradleEnv()).callModule('pip', 'install extract-msg --upgrade --user')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'extract-msg:0.28.0' // version does not matter here
            }

        """

        when: "run task"
        BuildResult result = run('pipUpdates')

        then: "nothing to update"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('All modules use the most recent versions')
    }

    def "Check updates for all"() {

        setup:
        // use the latest version
        new Pip(gradleEnv()).install('extract-msg==0.28.0')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
            }
            
            pipUpdates.all = true

        """

        when: "run task"
        BuildResult result = run('pipUpdates')

        then: "nothing to update"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        !result.output.contains('All modules use the most recent versions')
    }


    def "Check no updates"() {

        setup:
        // empty environment
        env().create(false, true)

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
            }
            
            pipUpdates.all = true

        """

        when: "run task"
        BuildResult result = run('pipUpdates')

        then: "nothing to update"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        !result.output.contains('All modules use the most recent versions')
    }
}
