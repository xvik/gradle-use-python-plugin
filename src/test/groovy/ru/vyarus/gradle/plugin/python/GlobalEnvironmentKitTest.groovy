package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 05.04.2024
 */
class GlobalEnvironmentKitTest extends AbstractKitTest {

    @TempDir File envDir

    def "Check module override"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                envPath = "${envDir.absolutePath.replace('\\\\', '\\\\\\\\')}"
                pip 'extract-msg:0.28.1', 'extract-msg:0.28.0'
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
    }
}
