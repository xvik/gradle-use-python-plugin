package ru.vyarus.gradle.plugin.python

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv

/**
 * @author Vyacheslav Rusakov
 * @since 11.03.2020
 */
class VenvFromVenvCreationTest extends AbstractTest {

    def "Check venv creation correctness"() {

        setup:
        Project project = project()
        Virtualenv env = new Virtualenv(project, 'initial')
        env.create(true)

        // second, derived from first one
        Virtualenv env2 = new Virtualenv(project, env.pythonPath, null, "second")
        env2.python.extraArgs('-v') // enable logs
        Pip pip = new Pip(project, env.pythonPath, null, false)
        pip.install(env2.name)
        env2.createPythonOnly()

        when: "validating pip in second environment"
        Pip pip2 = new Pip(project, env2.pythonPath, null, false)

        then: "INCORRECT: pip exists, but it shouldn't"
        println pip2.version

    }
}
