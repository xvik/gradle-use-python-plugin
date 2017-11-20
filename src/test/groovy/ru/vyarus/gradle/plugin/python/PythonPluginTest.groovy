package ru.vyarus.gradle.plugin.python

import org.gradle.api.GradleException
import org.gradle.api.Project
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

        then: "pip task registered"
        project.tasks.getByName('pipInstall')
    }

    def "Check extension usage"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            python {
                pythonPath = 'foo/bar'
                pip 'sample:1', 'foo:2'
                showInstalledVersions = false
                alwaysInstallModules = true
            }

            task('pyt', type: PythonTask) {}
        }

        then: "pip task configured"
        def pipTask = project.tasks.getByName('pipInstall');
        pipTask.pythonPath == 'foo/bar'
        pipTask.modules == ['sample:1', 'foo:2']
        !pipTask.showInstalledVersions
        pipTask.alwaysInstallModules

        then: "python task configured"
        def pyTask = project.tasks.getByName('pyt');
        pyTask.pythonPath == 'foo/bar'
        pyTask.dependsOn.contains(project.tasks.getByName('pipInstall'))
    }


    def "Check python task misconfiguration"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            task ('pyt', type: PythonTask) {}
        }
        project.tasks.getByName('pyt').run()

        then: "validation failed"
        def ex = thrown(GradleException)
        ex.message == 'Module or command to execute must be defined'
    }

}