package ru.vyarus.gradle.plugin.python.task

import org.gradle.testfixtures.ProjectBuilder
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
        new Pip(ProjectBuilder.builder().build()).install('click==6.6')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'click:6.6'
            }

        """

        when: "run task"
        BuildResult result = run('pipUpdates')

        then: "click update detected"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('The following modules could be updated:')
        result.output =~ /click\s+6.6/
    }

    def "Check updates detected in environment"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'click:6.6'
            }

        """

        when: "install old version"
        BuildResult result = run('pipInstall')
        then: "installed"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip install click')


        when: "run task"
        result = run('pipUpdates')

        then: "click update detected"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('The following modules could be updated:')
        result.output =~ /click\s+6.6/
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
        new Python(ProjectBuilder.builder().build()).callModule('pip', 'install click --upgrade --user')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'click:6.7' // version does not matter here
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
        new Pip(ProjectBuilder.builder().build()).install('click==6.6')

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
