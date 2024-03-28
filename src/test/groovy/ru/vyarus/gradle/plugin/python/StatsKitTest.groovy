package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Pip

/**
 * @author Vyacheslav Rusakov
 * @since 28.03.2024
 */
class StatsKitTest extends AbstractKitTest {

    def "Check env plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
                
                printStats = true
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')

        unifyStats(result.output).contains("""task                                        started         duration            
:checkPython                                11:11:11:111    11ms                python3 --version
:checkPython                                11:11:11:111    11ms                python3 -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:checkPython                                11:11:11:111    11ms                python3 -m pip --version
:checkPython                                11:11:11:111    11ms                python3 -m pip show virtualenv
:checkPython                                11:11:11:111    11ms                python3 -m virtualenv --version
:checkPython                                11:11:11:111    11ms                python3 -m virtualenv .gradle/python
:checkPython                                11:11:11:111    11ms                .gradle/python/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:checkPython                                11:11:11:111    11ms                .gradle/python/bin/python -m pip --version
:pipInstall                                 11:11:11:111    11ms                .gradle/python/bin/python -m pip freeze
:pipInstall                                 11:11:11:111    11ms                .gradle/python/bin/python -m pip install extract-msg==0.28.0
:pipInstall                                 11:11:11:111    11ms                .gradle/python/bin/python -m pip list --format=columns
:sample                                     11:11:11:111    11ms                .gradle/python/bin/python -c exec("print('samplee')")

    Executed 12 commands in 11ms (overall)
""")
    }


    def "Check failed task"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                printStats = true
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c printt(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = runFailed('sample')

        then: "task failed"
        unifyStats(result.output).contains("""Python execution stats:

task                                        started         duration            
:checkPython                                11:11:11:111    11ms                python3 --version
:checkPython                                11:11:11:111    11ms                python3 -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:sample                                     11:11:11:111    11ms                FAILED   python3 -c exec("printt('samplee')")

    Executed 3 commands in 11ms (overall)
""")
    }

    def "Check list task"() {

        setup:
        // to show at least something
        new Pip(gradleEnv()).install('extract-msg==0.28.0')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER
            python.printStats = true
        """

        when: "run task"
        BuildResult result = run('pipList')

        then: "extract-msg update detected"
        result.task(':pipList').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip list --format=columns --user')
        result.output =~ /extract-msg\s+0.28.0/

        unifyStats(result.output).contains("""Python execution stats:

task                                        started         duration            
:checkPython                                11:11:11:111    11ms                python3 --version
:checkPython                                11:11:11:111    11ms                python3 -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:pipList                                    11:11:11:111    11ms                python3 -m pip list --format=columns --user

    Executed 3 commands in 11ms (overall)""")
    }


    def "Check updates detected in environment"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
                
                printStats = true
            }

        """

        when: "install old version"
        BuildResult result = run('pipInstall')
        then: "installed"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip install extract-msg')


        when: "run task"
        result = run('pipUpdates')

        then: "extract-msg update detected"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('The following modules could be updated:')
        result.output =~ /extract-msg\s+0.28.0/

        unifyStats(result.output).contains("""Python execution stats:

task                                        started         duration            
:checkPython                                11:11:11:111    11ms                python3 --version
:checkPython                                11:11:11:111    11ms                .gradle/python/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:checkPython                                11:11:11:111    11ms                .gradle/python/bin/python -m pip --version
:pipUpdates                                 11:11:11:111    11ms                .gradle/python/bin/python -m pip list -o -l --format=columns

    Executed 4 commands in 11ms (overall)""")
    }
}
