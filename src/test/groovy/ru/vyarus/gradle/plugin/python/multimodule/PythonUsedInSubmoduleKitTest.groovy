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
                
                tasks.register('sample', PythonTask) {
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
                                                                                
                tasks.register('sample', PythonTask) {
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
                tasks.register('sample', PythonTask) {
                    command = '-c print(\\'samplee\\')'
                }
            }
            
            python {
                pip 'extract-msg:0.28.0'
            }
            
            tasks.register('rsample', PythonTask) {
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

    def "Check all modules use plugin at the same build"() {

        setup:
        file('settings.gradle') << ' include "sub"'
        file('sub').mkdir()
        build """                        
            plugins {
                id 'ru.vyarus.use-python' apply false
            }
            
            allprojects {
               apply plugin: 'ru.vyarus.use-python'
               
               tasks.register('sample', PythonTask) {
                    command = "-c print('samplee\${project.path}')"
                } 
            }                        
            
            python {
                pip 'extract-msg:0.28.0'
            }
            
        """

        when: "run root task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee:')

        then: "sub task successful"
        result.task(':sub:sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('samplee:sub')
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
                                                                                           
                tasks.register('sample', PythonTask) {
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
                                                                                     
                tasks.register('sample', PythonTask) {
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
