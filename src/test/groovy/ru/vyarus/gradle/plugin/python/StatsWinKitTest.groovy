package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Pip
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 30.03.2024
 */
@IgnoreIf({ !System.getProperty("os.name").toLowerCase().contains("windows") })
class StatsWinKitTest extends AbstractKitTest {

    // IMPORTANT synchronize changes with StatsKitTest!

    def "Check env plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
                useVenv = false
                
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
:checkPython                                11:11:11:111    11ms                python -c "import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)"
:checkPython                                11:11:11:111    11ms                python -m pip --version
:checkPython                                11:11:11:111    11ms                python -m pip show virtualenv
:checkPython                                11:11:11:111    11ms                python -m virtualenv --version
:checkPython                                11:11:11:111    11ms                python -m virtualenv .gradle/python
:checkPython                                11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -c "import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)"
:checkPython                                11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip --version
:pipInstall                                 11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip freeze
:pipInstall                                 11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip install extract-msg==0.28.0
:pipInstall                                 11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip list --format=columns
:sample                                     11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -c print('samplee')

    Executed 11 commands in 11ms (overall)
""")
    }

    def "Check venv plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
                useVenv = true
                
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
:checkPython                                11:11:11:111    11ms                python -c "import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)"
:checkPython                                11:11:11:111    11ms                python -m pip --version
:checkPython                                11:11:11:111    11ms                python -m venv -h
:checkPython                                11:11:11:111    11ms                python -m venv .gradle/python
:checkPython                                11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -c "import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)"
:checkPython                                11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip --version
:pipInstall                                 11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip freeze
:pipInstall                                 11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip install extract-msg==0.28.0
:pipInstall                                 11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip list --format=columns
:sample                                     11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -c print('samplee')

    Executed 10 commands in 11ms (overall)
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
        unifyStats(result.output).contains("""task                                        started         duration            
:checkPython                                11:11:11:111    11ms                python -c "import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)"
:sample                                     11:11:11:111    11ms                FAILED   python -c printt('samplee')

    Executed 2 commands in 11ms (overall)
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

        unifyStats(result.output).contains("""task                                        started         duration            
:checkPython                                11:11:11:111    11ms                python -c "import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)"
:pipList                                    11:11:11:111    11ms                python -m pip list --format=columns --user

    Executed 2 commands in 11ms (overall)""")
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

        unifyStats(result.output).contains("""task                                        started         duration            
:checkPython                                11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -c "import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)"
:checkPython                                11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip --version
:pipUpdates                                 11:11:11:111    11ms                cmd /c .gradle/python/Scripts/python.exe -m pip list -o -l --format=columns

    Executed 3 commands in 11ms (overall)""")
    }
}
