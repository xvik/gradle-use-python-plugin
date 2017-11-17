package ru.vyarus.gradle.plugin.python

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder

/**
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
class PythonPluginTest extends AbstractTest {

    def "Check extension registration"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply "ru.vyarus.use-python"

        then: "extension registered"
        project.extensions.findByType(PythonExtension)

    }

    def "Check extension validation"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            usePython {
                foo '1'
                bar '2'
            }
        }

        then: "validation pass"
        def usePython = project.extensions.usePython;
        usePython.foo == '1'
        usePython.bar == '2'
    }


    def "Check extension validation failure"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            usePython {
                foo '1'
            }
        }

        then: "validation failed"
        def ex = thrown(ProjectConfigurationException)
        ex.cause.message == 'usePython.bar configuration required'
    }

}