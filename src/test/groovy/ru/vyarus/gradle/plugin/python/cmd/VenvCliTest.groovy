package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.AbstractTest
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * @author Vyacheslav Rusakov
 * @since 02.04.2024
 */
class VenvCliTest extends AbstractTest {

    def "Check incorrect virtualenv creation"() {

        when: "create venv cli without path"
        new Venv(gradleEnv(), null)
        then: "error"
        thrown(IllegalArgumentException)
    }

    def "Check virtualenv detection"() {

        when: "call check env existence"
        Venv env = new Venv(gradleEnv(), 'env')
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
        Venv env = new Venv(gradleEnv(), 'env')
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
        Venv env = new Venv(gradleEnv(project), 'env')
        then: "path correct"
        env.path == 'env'
        env.pythonPath == CliUtils.canonicalPath(project.rootDir.absolutePath, isWin ? 'env/Scripts' : 'env/bin')
    }
}
