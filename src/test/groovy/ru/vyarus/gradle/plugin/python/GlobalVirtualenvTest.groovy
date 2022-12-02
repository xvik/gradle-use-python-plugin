package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv

/**
 * @author Vyacheslav Rusakov
 * @since 06.03.2020
 */
class GlobalVirtualenvTest extends AbstractKitTest {

    def "Check env plugin execution"() {
        setup:
        // create virtualenv and use it as "global" python
        // without extra detection, plugin will try to use --user flag for virtualenv installation and fail
        Virtualenv env = env('env')
        env.create(false)
        
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pythonPath = '${env.pythonPath.replace('\\', '\\\\')}'
                scope = VIRTUALENV
                pip 'extract-msg:0.34.3'
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.34.3/
        result.output.contains('samplee')
    }
}
