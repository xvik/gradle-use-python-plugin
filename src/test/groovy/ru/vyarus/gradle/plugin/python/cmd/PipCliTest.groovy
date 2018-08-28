package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.AbstractTest

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
        pip.version =~ /\d+\.\d+(\.\d+)?/
        pip.versionLine =~ /pip \d+\.\d+(\.\d+)? from/
    }

    def "Check version parse fail"() {

        when: "prepare pip"
        Project project = project()
        Pip pip = new FooPip(project)
        then: "ok"
        pip.version =~ /\d+\.\d+(\.\d+)?/

    }

    class FooPip extends Pip {

        FooPip(Project project) {
            super(project)
        }

        @Override
        String getVersionLine() {
            return 'you will not parse it'
        }
    }
}
