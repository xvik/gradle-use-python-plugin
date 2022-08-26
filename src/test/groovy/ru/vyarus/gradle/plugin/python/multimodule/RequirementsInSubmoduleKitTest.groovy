package ru.vyarus.gradle.plugin.python.multimodule

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 26.08.2022
 */
class RequirementsInSubmoduleKitTest extends AbstractKitTest {

    def "Check requirements works in submodule"() {

        setup:
        file('settings.gradle') << ' include "sub"'
        file('sub').mkdir()
        build """
            plugins {
                id 'ru.vyarus.use-python' apply false
            }
                        
            subprojects {
                apply plugin: 'ru.vyarus.use-python'                
            }

        """
        file('sub/').mkdir()
        file('sub/requirements.txt') << """   
# comment
extract-msg == 0.34.3
"""

        when: "run task"
        BuildResult result = run(':sub:pipInstall')

        then: "task successful"
        result.task(':sub:pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv ../.gradle/python')
        result.output =~ /extract-msg\s+0.34.3/

        then: "virtualenv created at the root level"
        result.output.contains("${projectName()}${File.separator}.gradle${File.separator}python")
    }
}
