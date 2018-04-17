package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import ru.vyarus.gradle.plugin.python.AbstractTest

import java.util.regex.Pattern

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipCliTest extends AbstractTest {

    def "Check pip cli usage"() {

        when: "call pip"
        Project project = project()
        Pip pip = new Pip(project)
        pip.exec('list')
        then: 'ok'
        true

        when: "pip install"
        pip.install('click==6.7')
        then: "ok"
        pip.isInstalled('click')

        when: "pip uninstall"
        pip.uninstall('click')
        then: "ok"
        !pip.isInstalled('click')
    }

    def "Check pip utils"() {

        when: "call pip"
        Project project = project()
        Pip pip = new Pip(project)
        pip.exec('list')
        then: "ok"
        pip.version =~ /\d+\.\d+\.\d+/
        pip.versionLine =~ /pip \d+\.\d+\.\d+ from/
    }

    def "Check version parse fail"() {

        when: "fallback to python call"
        Project project = project()
        Pip pip = new FooPip(project, 'you will not parse it')
        then: "ok"
        pip.version =~ /\d+\.\d+\.\d+/

        when: "empty version in regex fallback to python"
        pip = new FooPip(project, 'pip form ')
        then: "ok"
        pip.version =~ /\d+\.\d+\.\d+/

    }

    class FooPip extends Pip {

        String line

        FooPip(Project project, String line) {
            super(project)
            this.line = line
        }

        @Override
        String getVersionLine() {
            return line
        }
    }
}
