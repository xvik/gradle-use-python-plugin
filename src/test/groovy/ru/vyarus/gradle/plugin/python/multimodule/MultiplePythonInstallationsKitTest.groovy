package ru.vyarus.gradle.plugin.python.multimodule

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 28.08.2018
 */
class MultiplePythonInstallationsKitTest extends AbstractKitTest {

    def "Check modules use different python"() {

        setup:
        file('settings.gradle') << ' include "sub1", "sub2"'
        file('sub1').mkdir()
        file('sub2').mkdir()
        build """
            plugins {
                id 'ru.vyarus.use-python' apply false
            }

            subprojects {
                apply plugin: 'ru.vyarus.use-python'
                python {                    
                    envPath = 'python' // relative to module!

                    // here different python version could be configured, but for test it's not important
                    
                    pip 'extract-msg:0.28.0'
                }
                
                task sample(type: PythonTask) {
                    command = '-c print(\\'samplee\\')'
                }                
            }                                    
        """

        when: "run module 1 task"
        BuildResult result = run(':sub1:sample')

        then: "task successful"
        result.task(':sub1:sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
        result.output.contains("${projectName()}${File.separator}sub1${File.separator}python")


        when: "run module 2 task"
        result = run(':sub2:sample')

        then: "task successful"
        result.task(':sub2:sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
        result.output.contains("${projectName()}${File.separator}sub2${File.separator}python")
    }
}
