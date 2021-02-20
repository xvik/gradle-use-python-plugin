package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * @author Vyacheslav Rusakov
 * @since 28.08.2018
 */
class AbsoluteVirtualenvLocationKitTest extends AbstractKitTest {

    @Rule
    final TemporaryFolder envDir = new TemporaryFolder()

    def "Check virtualenv configuration with absolute path"() {

        setup:
        build """    
            plugins {
                id 'ru.vyarus.use-python'
            }
                        
            python {
                envPath = "${CliUtils.canonicalPath(envDir.root).replace('\\', '\\\\')}"

                pip 'extract-msg:0.28.0'
            }
                                                                                
            task sample(type: PythonTask) {
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
        result.output.contains("${CliUtils.canonicalPath(envDir.root)}${File.separator}")
    }
}
