package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv

/**
 * @author Vyacheslav Rusakov
 * @since 24.05.2018
 */
class PipUpgradeTest extends AbstractKitTest {

    def "Check pip local upgrade"() {

        setup:
        Virtualenv env = env()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python {
                pip 'pip:20.3.4'
                pip 'extract-msg:0.34.3'
                
                alwaysInstallModules = true
            }
            
        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "pip installed"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip==20.3.4')

        when: "run one more time to check used pip"
        result = run('pipInstall')
        then: "pip 20 used"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('Using pip 20.3.4 from')
    }
}
