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
        Project project = project()
        Python python = new Python(project)
        def res = python.readOutput('-c "print(\'hello\')"')
        then: "ok"
        res == 'hello'

        when: 'check home dir'
        res = python.getHomeDir()
        then: "ok"
        res

        when: 'check version'
        res = python.getVersion()
        then: "ok"
        res ==~ /\d+\.\d+\.\d+/

        and: 'check virtualenv detection'
        !python.virtualenv

        when: 'check simple exec'
        python.exec('-c "print(\'hello\')"')
        then: "ok"
        true
    }

    def "Check error reporting"() {

        when: "call bad command"
        Project project = project()
        Python python = new Python(project)
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
        Project project = project()
        Python python = new Python(project)

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

        when: "set python args"
        python.pythonArgs('--arg')
        then: 'set'
        python.pythonArgs == ['--arg']

        when: "append python args"
        python.pythonArgs(['--arg2', '--arg3'])
        then: 'set'
        python.pythonArgs == ['--arg', '--arg2', '--arg3']

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

        when: "clear python args"
        python.clearPythonArgs()
        then: 'set'
        python.pythonArgs.empty

        when: "clear args"
        python.clearExtraArgs()
        then: 'set'
        python.extraArgs.empty

        when: "set vars"
        python.environment('foo', 1)
        python.environment('bar', 2)
        then: 'set'
        python.envVars == ['foo': 1, 'bar' : 2]

        when: 'clear vars'
        python.clearEnvironment()
        then: 'no vars'
        python.envVars.size() == 0

        when: 'mass variables set'
        python.environment(['foo': 2, 'bar' : 3])
        python.environment(['foo': 1, 'baz' : 4])
        then: 'aggregated'
        python.envVars == ['foo': 1, 'bar' : 3, 'baz': 4]

        when: 'additional var'
        python.environment('sample': 'sam')
        then: 'aggregated'
        python.envVars == ['foo': 1, 'bar' : 3, 'baz': 4, 'sample': 'sam']

    }
}