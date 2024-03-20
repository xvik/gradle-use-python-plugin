package ru.vyarus.gradle.plugin.python.task


import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import ru.vyarus.gradle.plugin.python.cmd.Pip

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipInstallTaskKitTest extends AbstractKitTest {

    @Override
    def setup() {
        // make sure correct version installed
        new Pip(gradleEnv()).install('extract-msg==0.28.0')
    }

    def "Check no declared modules"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "no all modules list printed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SKIPPED
    }

    def "Check no modules list"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'extract-msg:0.28.0'
                scope = USER
                showInstalledVersions = false
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "no all modules list printed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        !result.output.contains('python -m pip install extract-msg')
        !result.output.contains('python -m pip list')

        when: "run one more time"
        result = run('pipInstall')

        then: "up to date"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS // up to date check removed
    }

    def "Check always install"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'extract-msg:0.28.0'
                alwaysInstallModules = true
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "extract-msg install called"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('Requirement already satisfied: extract-msg==0.28.0')
        result.output =~ /python(3)? -m pip list/
    }

    def "Check custom task"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER
            
            tasks.register('customPip', PipInstallTask) {                
                pip 'extract-msg:0.28.0'
                alwaysInstallModules = true
            }

        """

        when: "run task"
        BuildResult result = run('customPip')

        then: "extract-msg install called"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':customPip').outcome == TaskOutcome.SUCCESS
        result.output.contains('Requirement already satisfied: extract-msg==0.28.0')
        result.output =~ /python(3)? -m pip list/
    }

    def "Check extra index urls and trusted hosts options "() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python {
                scope = USER
                pip 'extract-msg:0.28.0'
                alwaysInstallModules = true
                extraIndexUrls "http://extra-url.com"
                trustedHosts "extra-url.com" 
            }
        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "arguments applied"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /python(3)? -m pip install extract-msg==0.28.0 --user --extra-index-url http:\/\/extra-url.com --trusted-host extra-url.com/
    }

    def "Check applying custom arguments"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python {
                scope = USER
                pip 'extract-msg:0.28.0'
                alwaysInstallModules = true
            }
            
            pipInstall.options('--upgrade-strategy', 'only-if-needed')
        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "arguments applied"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /python(3)? -m pip install extract-msg==0.28.0 --user --upgrade-strategy only-if-needed/
    }
}
