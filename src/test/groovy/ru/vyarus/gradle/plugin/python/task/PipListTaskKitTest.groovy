package ru.vyarus.gradle.plugin.python.task

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import ru.vyarus.gradle.plugin.python.cmd.Pip

/**
 * @author Vyacheslav Rusakov
 * @since 15.12.2017
 */
class PipListTaskKitTest extends AbstractKitTest {

    def "Check list task"() {

        setup:
        // to show at least something
        new Pip(ProjectBuilder.builder().build()).install('extract-msg==0.28.0')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER
        """

        when: "run task"
        BuildResult result = run('pipList')

        then: "extract-msg update detected"
        result.task(':pipList').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip list --format=columns --user')
        result.output =~ /extract-msg\s+0.28.0/
    }

    def "Check list all task"() {

        setup:
        // to show at least something
        new Pip(ProjectBuilder.builder().build()).install('extract-msg==0.28.0')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER
            
            pipList.all = true
        """

        when: "run task"
        BuildResult result = run('pipList')

        then: "extract-msg update detected"
        result.task(':pipList').outcome == TaskOutcome.SUCCESS
        !result.output.contains('pip list --format=columns --user')
        result.output =~ /extract-msg\s+0.28.0/
    }
}
