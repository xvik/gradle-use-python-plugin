package ru.vyarus.gradle.plugin.python.multimodule

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv

/**
 * @author Vyacheslav Rusakov
 * @since 08.09.2022
 */
class ParallelExecutionKitTest extends AbstractKitTest {

    def "Check concurrent virtualenv installation"() {

        setup:
        // need source python without virtualenv
        Virtualenv env = env('env')
        env.create()

        build("""
            plugins {
                id 'ru.vyarus.use-python'
            }     

            subprojects {
                apply plugin: 'ru.vyarus.use-python'
                
                // NOTE inner virtualenv would be created at the root project!
                python.pythonPath = "../${env.pythonPath.replace("\\", "\\\\")}"    
                python.pip 'extract-msg:0.28.0'  
            }
        """)

        // amount of modules in test project
        int cnt = 20

        file('settings.gradle') << ' include ' + (1..cnt).collect {
            // work dir MUST exist otherwise process will fail to start!
            assert file("mod$it").mkdir()
            return "'mod$it'" 
        }.join(',')

        when: "run check all modules to initiate concurrent virtualenv installation"
        debug()
        BuildResult result = run('checkPython', '--parallel', '--max-workers=5')

        then: "tasks successful"
        (1..cnt).collect {
            result.task(":mod$it:checkPython").outcome == TaskOutcome.SUCCESS
        }
    }
}
