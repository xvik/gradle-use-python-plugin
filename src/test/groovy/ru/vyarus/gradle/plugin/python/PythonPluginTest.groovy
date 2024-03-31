package ru.vyarus.gradle.plugin.python

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import ru.vyarus.gradle.plugin.python.task.PythonTask
import ru.vyarus.gradle.plugin.python.task.pip.PipInstallTask
import ru.vyarus.gradle.plugin.python.task.pip.PipListTask
import ru.vyarus.gradle.plugin.python.task.pip.PipUpdatesTask
import ru.vyarus.gradle.plugin.python.task.pip.module.VcsPipModule

/**
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
class PythonPluginTest extends AbstractTest {

    def "Check extension registration"() {

        when: "plugin applied"
        Project project = project()
        project.plugins.apply "ru.vyarus.use-python"

        then: "extension registered"
        project.extensions.findByType(PythonExtension)

        then: "pip task registered"
        project.tasks.getByName('checkPython')
        project.tasks.getByName('pipInstall')
        project.tasks.getByName('pipUpdates')
        project.tasks.getByName('pipList')
        project.tasks.getByName('cleanPython')
    }

    def "Check extension usage"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            python {
                pythonPath = 'foo/bar'
                pythonBinary = 'py'
                scope = GLOBAL
                pip 'sample:1', 'foo:2'
                showInstalledVersions = false
                alwaysInstallModules = true
            }

            task('pyt', type: PythonTask) {}
        }

        then: "pip install task configured"
        PipInstallTask pipTask = project.tasks.getByName('pipInstall');
        pipTask.pythonPath.get() == 'foo/bar'
        pipTask.pythonBinary.get() == 'py'
        !pipTask.userScope.get()
        pipTask.modules.get() == ['sample:1', 'foo:2']
        !pipTask.showInstalledVersions.get()
        pipTask.alwaysInstallModules.get()

        then: "python task configured"
        PythonTask pyTask = project.tasks.getByName('pyt');
        pyTask.pythonPath.get() == 'foo/bar'
        pyTask.pythonBinary.get() == 'py'
        pyTask.dependsOn.collect {it.name}.contains('pipInstall')

        then: "pip updates task configured"
        PipUpdatesTask pipUpdates = project.tasks.getByName('pipUpdates');
        pipUpdates.pythonPath.get() == 'foo/bar'
        pipUpdates.pythonBinary.get() == 'py'
        !pipUpdates.userScope.get()
        pipUpdates.modules.get() == ['sample:1', 'foo:2']

        then: "pip list task configured"
        PipListTask pipList = project.tasks.getByName('pipList');
        pipList.pythonPath.get() == 'foo/bar'
        pipList.pythonBinary.get() == 'py'
        !pipList.userScope.get()
        pipList.modules.get() == ['sample:1', 'foo:2']

        then: "clean task configured"
        Delete clean = project.tasks.getByName('cleanPython')
        clean.delete == ['.gradle/python'.replace('/', File.separator)] as Set
    }


    def "Check python task misconfiguration"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            task('pyt', type: PythonTask) {}
        }
        project.tasks.getByName('pyt').run()

        then: "validation failed"
        def ex = thrown(GradleException)
        ex.message == 'Module or command to execute must be defined'
    }

    def "Check module declaration util"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            python.pip 'sample:1', 'foo:2'
        }

        then: "modules check correct"
        def ext = project.extensions.getByType(PythonExtension)
        ext.isModuleDeclared('sample')
        ext.isModuleDeclared('foo')
        !ext.isModuleDeclared('sampleee')
    }

    def "Check modules override"() {

        when: "vcs declaration override normal"
        Project project = project {
            apply plugin: "ru.vyarus.use-python"

            python.pip 'foo:1',
                        'git+https://git.example.com/foo@v2.0#egg=foo-2'
        }
        def res = project.tasks.getByName('pipInstall').getModulesList()

        then: "one module"
        res.size() == 1
        res[0] instanceof VcsPipModule
        res[0].toPipString() == "foo @ git+https://git.example.com/foo@v2.0"


        when: "opposite override"
        project = super.project {
            apply plugin: "ru.vyarus.use-python"

            python.pip  'git+https://git.example.com/foo@v2.0#egg=foo-2',
                    'foo:1'
        }
        res = project.tasks.getByName('pipInstall').getModulesList()

        then: "one module"
        res.size() == 1
        !(res[0] instanceof VcsPipModule)
        res[0].toPipString() == "foo==1"
    }
}