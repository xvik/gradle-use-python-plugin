package ru.vyarus.gradle.plugin.python.task

import org.gradle.testkit.runner.BuildResult
import ru.vyarus.gradle.plugin.python.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 13.12.2017
 */
class CheckTaskKitTest extends AbstractKitTest {

    def "Check no python"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pythonPath = 'somewhere'
            }

        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "task successful"
        result.output.contains('Python not found: somewhere/python')
    }

    def "Check python version requirement"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                minPythonVersion = '5'
            }

        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "task successful"
        result.output.contains('does not match minimal required version: 5')
    }


    def "Check pip version requirement"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                minPipVersion = '10.1'
                
                pip 'mod:1'
            }

        """

        when: "run task"
        BuildResult result = runFailed('checkPython')

        then: "task successful"
        result.output.contains('does not match minimal required version: 10.1')
    }
}
