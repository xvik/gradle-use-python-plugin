package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.cmd.Pip
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 14.03.2024
 */
class ConfigurationCacheSupportKitTest extends AbstractKitTest {

    def "Check simple plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'extract-msg:0.28.0'
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'sample')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'sample')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }

    def "Check env plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'sample')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'sample')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }

    def "Check exact virtualenv version installation"() {
        setup:
        Virtualenv env = env('env')
        env.create()
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = VIRTUALENV
                pip 'extract-msg:0.28.0'
                pythonPath = "${env.pythonPath.replace('\\', '\\\\')}"
                virtualenvVersion = "20.24.6"
            }            
        """

        when: "run task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'checkPython')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task successful"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains("-m pip install virtualenv==20.24.6")


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'checkPython')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        !result.output.contains('-m pip install virtualenv==20.24.6')
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
        """

        when: "run task"
        println "\n\n----------------------------------------------------------------------------------"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipList')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "extract-msg update detected"
        result.task(':pipList').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip list --format=columns --user')
        result.output =~ /extract-msg\s+0.28.0/


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipList')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':pipList').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip list --format=columns --user')
        result.output =~ /extract-msg\s+0.28.0/
    }


    def "Check updates detected"() {

        setup:
        // make sure old version installed
        new Pip(gradleEnv()).install('extract-msg==0.28.0')

        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'extract-msg:0.28.0'
            }

        """

        when: "run task"
        println "\n\n----------------------------------------------------------------------------------"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipUpdates')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "extract-msg update detected"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('The following modules could be updated:')
        result.output =~ /extract-msg\s+0.28.0/



        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipUpdates')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
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
            }

        """

        when: "install old version"
        BuildResult result = run('pipInstall')
        then: "installed"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('pip install extract-msg')


        when: "run task"
        println "\n\n----------------------------------------------------------------------------------"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipUpdates')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "extract-msg update detected"
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
        result.output.contains('The following modules could be updated:')
        result.output =~ /extract-msg\s+0.28.0/


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipUpdates')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':pipUpdates').outcome == TaskOutcome.SUCCESS
    }


    def "Check strict requirements support"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3

# vcs syntax (note, it's not valid syntax for pip due to version in egg part!) 
git+https://github.com/ictxiangxin/boson/@ea7d9113f71a7eb79083208d4f3bbb74feeb149f#egg=boson-1.4

# features syntax
requests[socks,security] == 2.28.1
"""

        when: "run task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipInstall')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        result.output =~ /extract-msg\s+0.34.3/
        result.output =~ /boson\s+1.4/
        result.output =~ /requests\s+2.28.1/


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'pipInstall')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('3 modules to install read from requirements file: requirements.txt (strict mode)')
        result.output.contains('All required modules are already installed with correct versions')
    }


    // testcontainers doesn't work on windows server https://github.com/testcontainers/testcontainers-java/issues/2960
    @IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
    def "Check docker simple execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true                
                environment 'PYTHON_ENV_TEST', 'IN-CONTAINER'
            }

            // use environment variable to make sure python executed in docker            
            tasks.register('sample', PythonTask) {
                doFirst {
                    println 'OUTER ENV: ' + System.getenv('PYTHON_ENV_TEST')
                }
                command = '-c "import os; print(\\'CONTAINER ENV: \\' + str(os.getenv(\\'PYTHON_ENV_TEST\\')))"'
            }

        """

        when: "run task"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'sample')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output.contains('OUTER ENV: null')
        result.output.contains('CONTAINER ENV: IN-CONTAINER')

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'sample')

        then: "cache used"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('Reusing configuration cache.')
        result.output.contains('OUTER ENV: null')
        result.output.contains('CONTAINER ENV: IN-CONTAINER')
    }
}
