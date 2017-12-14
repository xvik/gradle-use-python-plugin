package ru.vyarus.gradle.plugin.python.cmd

import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PythonExecTest extends AbstractCliMockSupport {

    Python python

    @Override
    void setup() {
        python = new Python(project)
    }

    def "Check success"() {
        setup:
        mockExec(project, 'sample output', 0)

        when: "check read output"
        def res = python.readOutput('no matter')
        then: "ok"
        res == 'sample output'

        when: "check exec"
        logger.reset()
        python.exec('no matter')
        then: "ok"
        logger.res =~ /(?m)\[python] python(3)? no matter\n\t sample output\n/
    }

    def "Check fail"() {
        setup:
        mockExec(project, 'sample output', 1)

        when: "check failed exec"
        python.exec('no matter')
        then: "fail"
        def ex = thrown(PythonExecutionFailed)
        ex.message =~ /python(3)? no matter/
        logger.res =~ /(?m)\[python] python(3)? no matter\n\t sample output\n/

        when: "check failed read output"
        logger.reset()
        python.exec('no matter')
        then: "fail"
        ex = thrown(PythonExecutionFailed)
        ex.message =~ /python(3)? no matter/
        logger.res =~ /(?m)\[python] python(3)? no matter\n\t sample output\n/
    }

    def "Check extra args"() {
        setup:
        python.extraArgs('--one --two')
        mockExec(project, 'sample output', 0)

        when: "check exec"
        logger.reset()
        python.exec('no matter')
        then: "ok"
        logger.res =~ /(?m)\[python] python(3)? no matter --one --two\n\t sample output\n/

        cleanup:
        python.clearExtraArgs()
    }

    def "Check module call"() {

        setup:
        mockExec(project, 'sample output', 0)

        when: "call module"
        python.callModule('mmm', '--ha --ha')
        then: "ok"
        logger.res =~ /(?m)\[python] python(3)? -m mmm --ha --ha\n\t sample output\n/
    }

    def "Check python path appliance"() {

        setup:
        mockExec(project, 'sample output', 0)
        python = new Python(project, 'some/path', null)
        file('some/path').mkdirs()
        file('some/path/python.exe').createNewFile()

        when: "call module"
        python.exec('mmm')
        then: "ok"
        logger.res == (isWin ? "[python] cmd /c some\\path\\python.exe mmm\n\t sample output\n" : "[python] some/path/python mmm\n\t sample output\n")
    }

    def "Check python binary change"() {

        setup:
        mockExec(project, 'sample output', 0)
        python = new Python(project, null, 'py')

        when: "call module"
        python.exec('mmm')
        then: "ok"
        logger.res == "[python] py mmm\n\t sample output\n"
    }

    def "Check custom path and binary"() {

        setup:
        mockExec(project, 'sample output', 0)
        python = new Python(project, 'some/path', 'py')
        file('some/path').mkdirs()
        file('some/path/py.exe').createNewFile()

        when: "call module"
        python.exec('mmm')
        then: "ok"
        logger.res == (isWin ? "[python] cmd /c some\\path\\py.exe mmm\n\t sample output\n" : "[python] some/path/py mmm\n\t sample output\n")
    }
}
