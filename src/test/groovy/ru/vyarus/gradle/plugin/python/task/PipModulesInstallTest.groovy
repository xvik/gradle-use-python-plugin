package ru.vyarus.gradle.plugin.python.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 19.05.2018
 */
class PipModulesInstallTest extends AbstractKitTest {

    def "Check vcs install"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'git+https://github.com/ictxiangxin/boson/@b52727f7170acbedc5a1b4e1df03972bd9bb85e3#egg=boson-0.9'
                usePipCache = false
                useVenv = false
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "package install called"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('Successfully built boson')
        result.output.contains('boson-0.9')

        when: "second install"
        result = run('pipInstall')
        then: "package not installed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS // up to date check removed
        !result.output.contains('Successfully built boson')
        !result.output.contains('boson-0.9')
    }


    def "Check vcs install venv"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'git+https://github.com/ictxiangxin/boson/@b52727f7170acbedc5a1b4e1df03972bd9bb85e3#egg=boson-0.9'
                usePipCache = false
                useVenv = true
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "package install called"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.replace('MarkupSafe-2.1.5', 'MarkupSafe-3.0.1').contains('Successfully installed MarkupSafe-3.0.1 boson-0.9')
        result.output.contains('boson-0.9')

        when: "second install"
        result = run('pipInstall')
        then: "package not installed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS // up to date check removed
        !result.output.replace('MarkupSafe-2.1.5', 'MarkupSafe-3.0.1').contains('Successfully installed MarkupSafe-3.0.1 boson-0.9')
        !result.output.contains('boson-0.9')
    }

    def "Check square syntax"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'requests[socks,security]:2.18.4'
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "package install called"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('requests-2.18.4')

        when: "second install"
        result = run('pipInstall')
        then: "package not installed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS // up to date check removed
    }
}
