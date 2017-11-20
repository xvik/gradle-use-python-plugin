package ru.vyarus.gradle.plugin.python.cmd

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipExecTest extends AbstractCliMockSupport {

    Pip pip

    @Override
    void setup() {
        pip = new Pip(project, null)
    }

    def "Check execution"() {
        setup:
        mockExec(project, 'sample output', 0)

        when: "call install module"
        pip.install('mod')
        then: "ok"
        logger.res == '[python] python -m pip install mod\n\t sample output\n'

        when: "call pip cmd"
        logger.reset()
        pip.exec('list --format')
        then: "ok"
        logger.res == '[python] python -m pip list --format\n\t sample output\n'
    }
}
