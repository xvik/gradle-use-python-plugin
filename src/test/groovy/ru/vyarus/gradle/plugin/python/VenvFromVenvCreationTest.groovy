package ru.vyarus.gradle.plugin.python

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * @author Vyacheslav Rusakov
 * @since 11.03.2020
 */
class VenvFromVenvCreationTest extends AbstractTest {

    def "Check venv creation correctness"() {

        setup:
        Project project = project()
        Virtualenv env = new Virtualenv(gradleEnv(project), 'initial')
        env.create(true)

        // second, derived from first one
        Virtualenv env2 = new Virtualenv(gradleEnv(project), env.pythonPath, null, "second")
        env2.python.extraArgs('-v') // enable logs
        Pip pip = new Pip(gradleEnv(project), env.pythonPath, null).userScope(false)
        pip.install(env2.name + "==20.24.6")
        env2.createPythonOnly()

        when: "validating pip in second environment"
        Pip pip2 = new Pip(gradleEnv(project), env2.pythonPath, null).userScope(false)
        println pip2.version

        then: "pip not exists"
        thrown(PythonExecutionFailed)
    }
}
