package ru.vyarus.gradle.plugin.python.docker

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 07.10.2022
 */
// testcontainers doesn't work on windows server https://github.com/testcontainers/testcontainers-java/issues/2960
@IgnoreIf({ env.containsKey('APPVEYOR') })
class DockerMultiModuleKitTest extends AbstractKitTest {

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
                    docker.use = true                    
                }
                
                task sample(type: PythonTask) {
                    command = "-c print('sampl\${project.name}')"
                }                
            }                                    
        """

        when: "run python tasks in all modules"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sub1:sample').outcome == TaskOutcome.SUCCESS
        result.task(':sub2:sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('samplsub1')
        result.output.contains('samplsub2')
    }

    def "Check parallel execution"() {

        build("""
            plugins {
                id 'ru.vyarus.use-python'
            }     

            subprojects {
                apply plugin: 'ru.vyarus.use-python'
                python {
                    docker.use = true                   
                }
                
                task sample(type: PythonTask) {
                    command = "-c print('sampl\${project.name}')"
                }                                    
            }
        """)

        // amount of modules in test project
        int cnt = 20

        file('settings.gradle') << ' include ' + (1..cnt).collect {
            // work dir MUST exist otherwise process will fail to start!
            assert file("mod$it").mkdir()
            return "'mod$it'"
        }.join(',')

        when: "run python tasks in all modules"
        debug()
        BuildResult result = run('sample', '--parallel', '--max-workers=5')

        then: "tasks successful"
        (1..cnt).collect {
            result.task(":mod$it:sample").outcome == TaskOutcome.SUCCESS
        }
    }
}
