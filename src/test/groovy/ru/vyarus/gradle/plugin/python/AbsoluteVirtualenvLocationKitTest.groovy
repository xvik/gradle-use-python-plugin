package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.util.CliUtils
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 28.08.2018
 */
class AbsoluteVirtualenvLocationKitTest extends AbstractKitTest {

    @TempDir File envDir

    def "Check virtualenv configuration with absolute path"() {

        setup:
        build """    
            plugins {
                id 'ru.vyarus.use-python'
            }
                        
            python {
                envPath = "${CliUtils.canonicalPath(envDir).replace('\\', '\\\\')}"

                pip 'extract-msg:0.28.0'
            }
                                                                                
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }                        

        """

        when: "run task"
        BuildResult result = run(':sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')

        then: "virtualenv created at correct path"
        result.output.contains("${CliUtils.canonicalPath(envDir)}${File.separator}")
    }
}
