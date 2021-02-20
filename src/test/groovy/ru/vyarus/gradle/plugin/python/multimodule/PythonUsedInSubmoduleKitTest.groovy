package ru.vyarus.gradle.plugin.python.multimodule

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 28.08.2018
 */
class PythonUsedInSubmoduleKitTest extends AbstractKitTest {

    def "Check subproject applies plugin"() {

        setup:
        file('settings.gradle') << ' include "sub"'
        file('sub').mkdir()
        build """
            plugins {
                id 'ru.vyarus.use-python' apply false
            }
                        
            subprojects {
                apply plugin: 'ru.vyarus.use-python' 
                
                python {
                    pip 'extract-msg:0.28.0'
                }
                
                task sample(type: PythonTask) {
                    command = '-c print(\\'samplee\\')'
                }
            }

        """

        when: "run task"
        BuildResult result = run(':sub:sample')

        then: "task successful"
        result.task(':sub:sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')

        then: "virtualenv created at the root level"
        result.output.contains("${projectName()}${File.separator}.gradle${File.separator}python")
    }

    def "Check all modules applies plugin but used only in sub"() {

        setup:
        file('settings.gradle') << ' include "sub"'
        file('sub').mkdir()
        build """    
           
            plugins {
                id 'ru.vyarus.use-python' apply false
            }
            
            allprojects {
                apply plugin: 'ru.vyarus.use-python'
            }                        
            
            subprojects {
                        
                 python {
                    pip 'extract-msg:0.28.0'
                }
                                                                                
                task sample(type: PythonTask) {
                    command = '-c print(\\'samplee\\')'
                }
            }                        

        """

        when: "run task"
        BuildResult result = run(':sub:sample')

        then: "task successful"
        result.task(':sub:sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')

        then: "virtualenv created at the root level"
        result.output.contains("${projectName()}${File.separator}.gradle${File.separator}python")
    }


    def "Check all modules use plugin"() {

        setup:
        file('settings.gradle') << ' include "sub"'
        file('sub').mkdir()
        build """                        
            plugins {
                id 'ru.vyarus.use-python' apply false
            }
            
            allprojects {
               apply plugin: 'ru.vyarus.use-python' 
            }                        
            
            subprojects {                                                                           
                task sample(type: PythonTask) {
                    command = '-c print(\\'samplee\\')'
                }
            }
            
            python {
                pip 'extract-msg:0.28.0'
            }
            
            task rsample(type: PythonTask) {
                command = '-c print(\\'rsamplee\\')'
            }

        """

        when: "run root task"
        BuildResult result = run('rsample')

        then: "task successful"
        result.task(':rsample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('rsamplee')


        when: "run sub task"
        result = run(':sub:sample')

        then: "task successful"
        result.task(':sub:sample').outcome == TaskOutcome.SUCCESS
        !(result.output =~ /extract-msg\s+0.28.0/)
        result.output.contains('samplee')

        then: "virtualenv created at the root level"
        result.output.contains("${projectName()}${File.separator}.gradle${File.separator}python")
    }


    def "Check virtualenv relative to submodule"() {

        setup:
        file('settings.gradle') << ' include "sub"'
        file('sub').mkdir()
        build """                        
            plugins {
                id 'ru.vyarus.use-python' apply false
            }
            
            subprojects {
                apply plugin: 'ru.vyarus.use-python'
                
                python {
                    envPath = 'python'
                    
                    pip 'extract-msg:0.28.0'
                }
                                                                                           
                task sample(type: PythonTask) {
                    command = '-c print(\\'samplee\\')'
                }
            }                        
        """

        when: "run sub task"
        BuildResult result = run(':sub:sample')

        then: "task successful"
        result.task(':sub:sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')

        then: "virtualenv created at module level"
        !result.output.contains("${projectName()}${File.separator}.gradle${File.separator}python")
        result.output.contains("${projectName()}${File.separator}sub${File.separator}python")
    }

    def "Check submodule python call with custom work dir"() {

        setup:
        file('settings.gradle') << ' include "sub"'
        file('sub').mkdir()
        build """                        
            plugins {
                id 'ru.vyarus.use-python' apply false
            }
            
            allprojects {
               apply plugin: 'ru.vyarus.use-python' 
            }                        
            
            subprojects {  
                python {
                    pip 'extract-msg:0.28.0'
                }
                                                                                     
                task sample(type: PythonTask) {
                    command = '-c print(\\'samplee\\')'
                    workDir = 'src'
                }
            }
        """

        when: "run sub task"
        BuildResult result = run(':sub:sample')

        then: "task successful"
        result.task(':sub:sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('samplee')
    }
}
