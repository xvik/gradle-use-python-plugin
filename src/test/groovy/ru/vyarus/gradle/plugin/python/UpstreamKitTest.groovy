package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 05.03.2020
 */
class UpstreamKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '8.10.2'

    def "Check simple plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'extract-msg:0.28.0'
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = runVer(GRADLE_VERSION, 'sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }
}
