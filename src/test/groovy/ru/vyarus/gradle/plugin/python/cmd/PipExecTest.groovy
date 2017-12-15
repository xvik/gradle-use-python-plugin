package ru.vyarus.gradle.plugin.python.cmd

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipExecTest extends AbstractCliMockSupport {

    Pip pip

    @Override
    void setup() {
        pip = new Pip(project)
    }

    def "Check execution"() {
        setup:
        mockExec(project, 'sample output', 0)

        when: "call install module"
        pip.install('mod')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip install mod --user\n\t sample output\n/

        when: "call pip cmd"
        logger.reset()
        pip.exec('list --format')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip list --format --user\n\t sample output\n/

        when: "call freeze"
        logger.reset()
        pip.exec('freeze')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip freeze --user\n\t sample output\n/
    }
}
