package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.AbstractTest
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * @author Vyacheslav Rusakov
 * @since 14.12.2017
 */
class VirtualenvCliTest extends AbstractTest {

    void setup() {
        Pip pip = new Pip(gradleEnv())
        if (!pip.isInstalled(Virtualenv.PIP_NAME)) {
            pip.install(Virtualenv.PIP_NAME)
        }
    }

    def "Check incorrect virtualenv creation"() {

        when: "create virtualenv cli without path"
        new Virtualenv(gradleEnv(), null)
        then: "error"
        thrown(IllegalArgumentException)
    }

    def "Check virtualenv detection"() {

        when: "call check env existence"
        Virtualenv env = new Virtualenv(gradleEnv(), 'env')
        then: "env not exists"
        !env.exists()

        when: "empty env dir exists"
        file('env/').mkdir()
        then: "still no env"
        !env.exists()

        when: "at least one file in env"
        file('env/foo.txt').createNewFile()
        then: "detected"
        env.exists()
    }

    def "Check env creation"() {

        when: "create new env"
        Virtualenv env = new Virtualenv(gradleEnv(), 'env')
        assert !env.exists()
        env.createPythonOnly()
        then: "env created"
        env.exists()

        when: "create one more time"
        env.createPythonOnly()
        then: "nothing happen"
        env.exists()
    }

    def "Check util methods"() {

        when: "prepare virtualenv"
        Project project = project()
        Virtualenv env = new Virtualenv(gradleEnv(project), 'env')
        then: "path correct"
        env.path == 'env'
        env.pythonPath == CliUtils.canonicalPath(project.rootDir.absolutePath, isWin ? 'env/Scripts' : 'env/bin')

        then: "version correct"
        env.version =~ /\d+\.\d+\.\d+/
    }
}
