package ru.vyarus.gradle.plugin.python.cmd

import org.apache.tools.ant.taskdefs.condition.Os
import ru.vyarus.gradle.plugin.python.util.CliUtils

/**
 * @author Vyacheslav Rusakov
 * @since 10.03.2020
 */
class PipExecUnderVirtualenvTest extends AbstractCliMockSupport {

    Pip pip

    @Override
    void setup() {
        String root = dir.absolutePath
        String binPath = CliUtils.pythonBinPath(root, Os.isFamily(Os.FAMILY_WINDOWS))
        File bin = new File(binPath, 'activate')
        bin.mkdirs()
        bin.createNewFile() // force virtualenv detection
        assert bin.exists()
        execCase({ it.contains('sys.prefix') }, "3.5\n${root}\n${binPath + '/python3'}")
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
