package ru.vyarus.gradle.plugin.python.cmd


import ru.vyarus.gradle.plugin.python.AbstractTest
import ru.vyarus.gradle.plugin.python.cmd.env.Environment

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipCliTest extends AbstractTest {

    def "Check pip cli usage"() {

        when: "call pip"
        Pip pip = new Pip(gradleEnv())
        pip.exec('list')
        then: 'ok'
        true

        when: "pip install"
        pip.install('extract-msg==0.28.0')
        then: "ok"
        pip.isInstalled('extract-msg')

        when: "pip uninstall"
        pip.uninstall('extract-msg')
        then: "ok"
        !pip.isInstalled('extract-msg')
    }

    def "Check pip utils"() {

        when: "call pip"
        Pip pip = new Pip(gradleEnv())
        pip.exec('list')
        then: "ok"
        pip.version =~ /\d+\.\d+(\.\d+)?/
        pip.versionLine =~ /pip \d+\.\d+(\.\d+)? from/
    }

    def "Check version parse fail"() {

        when: "prepare pip"
        Pip pip = new FooPip(gradleEnv())
        then: "ok"
        pip.version =~ /\d+\.\d+(\.\d+)?/

    }

    class FooPip extends Pip {

        FooPip(Environment environment) {
            super(environment)
        }

        @Override
        String getVersionLine() {
            return 'you will not parse it'
        }
    }
}
