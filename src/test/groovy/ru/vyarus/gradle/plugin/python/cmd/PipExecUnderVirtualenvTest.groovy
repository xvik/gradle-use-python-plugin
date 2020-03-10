package ru.vyarus.gradle.plugin.python.cmd

/**
 * @author Vyacheslav Rusakov
 * @since 10.03.2020
 */
class PipExecUnderVirtualenvTest extends AbstractCliMockSupport {

    Pip pip

    @Override
    void setup() {
        // force virtualenv detection
        file("activate").createNewFile()
        execCase({ it.contains('sys.prefix') }, "3.5\n${dir.root.absolutePath}\n${dir.root.absolutePath}/python3")
        pip = new Pip(project)
    }

    def "Check execution"() {
        setup:
        mockExec(project, 'sample output', 0)

        when: "call install module"
        pip.install('mod')
        then: "user flag not set under virtualenv"
        pip.python.virtualenv
        logger.res =~ /\[python] python(3)? -m pip install mod\n\t sample output\n/

        when: "call pip cmd"
        logger.reset()
        pip.exec('list --format')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip list --format\n\t sample output\n/

        when: "call freeze"
        logger.reset()
        pip.exec('freeze')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip freeze\n\t sample output\n/
    }

}
