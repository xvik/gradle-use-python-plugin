package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import ru.vyarus.gradle.plugin.python.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipCliTest extends AbstractTest {

    def "Check pip cli usage"() {

        when: "call pip"
        Project project = ProjectBuilder.builder().build()
        Pip pip = new Pip(project)
        pip.exec('list')
        then: 'ok'
        true

        when: "pip install"
        pip.install('click==6.7')
        then: "ok"
        true

        when: "pip uninstall"
        pip.uninstall('click')
        then: "ok"
        true
    }
}
