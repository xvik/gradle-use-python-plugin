package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 28.03.2024
 */
class UseCustomPythonForTaskKitTest extends AbstractKitTest {

    def "Check env plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
            }
            
            tasks.register('sample', PythonTask) {
                // force global python usage instead of virtualenv
                pythonPath = null
                useCustomPython = true
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
        result.output =~ /(?m)\[python] python(3)? -c ${isWin ? 'print' : 'exec\\(\"print'}/
    }
}
