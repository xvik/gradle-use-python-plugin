package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.testfixtures.ProjectBuilder
import ru.vyarus.gradle.plugin.python.AbstractTest
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * @author Vyacheslav Rusakov
 * @since 18.11.2017
 */
class PythonCliTest extends AbstractTest {

    def "Check python execution"() {

        when: "Use default configuration"
        Project project = ProjectBuilder.builder().build()
        Python python = new Python(project, null)
        def res = python.readOutput('-c print(\'hello\')')
        then: "ok"
        res == 'hello'

        when: 'check home dir'
        res = python.getHomeDir()
        then: "ok"
        res != null

        when: 'check simple exec'
        python.exec('-c "print(\'hello\')"')
        then: "ok"
        true
    }

    def "Check error reporting"() {

        when: "call bad command"
        Project project = ProjectBuilder.builder().build()
        Python python = new Python(project, null)
        python.readOutput('-c "import fsdfdsfsd;"')
        then: "error"
        thrown(PythonExecutionFailed)

        when: "call bad exec"
        python.exec('-c "import fsdfdsfsd;"')
        then: "error"
        thrown(PythonExecutionFailed)
    }

    def "Check configuration"() {

        setup:
        Project project = ProjectBuilder.builder().build()
        Python python = new Python(project, null)

        when: "set output prefix"
        python.outputPrefix('[]')
        then: 'set'
        python.outputPrefix == '[]'

        when: "set null prefix"
        python.outputPrefix(null)
        then: 'set'
        python.outputPrefix == null

        when: "set log level"
        python.logLevel(LogLevel.DEBUG)
        then: 'set'
        python.logLevel == LogLevel.DEBUG

        when: "set null log level"
        python.logLevel(null)
        then: 'ignored'
        python.logLevel == LogLevel.DEBUG

        when: "set work dir"
        python.workDir('some/dir')
        then: 'set'
        python.workDir == 'some/dir'

        when: "set null work dir"
        python.workDir(null)
        then: 'ignored'
        python.workDir == 'some/dir'

        when: "set args"
        python.extraArgs('--arg')
        then: 'set'
        python.extraArgs == ['--arg']

        when: "append args"
        python.extraArgs(['--arg2', '--arg3'])
        then: 'set'
        python.extraArgs == ['--arg', '--arg2', '--arg3']

        when: "append null args"
        python.extraArgs(null)
        then: 'ignored'
        python.extraArgs == ['--arg', '--arg2', '--arg3']

        when: "clear args"
        python.clearExtraArgs()
        then: 'set'
        python.extraArgs.empty
    }
}