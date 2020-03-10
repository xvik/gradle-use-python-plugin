package ru.vyarus.gradle.plugin.python.util

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.internal.impldep.com.google.common.io.Files
import org.gradle.process.internal.ExecException
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class CliUtilsTest extends Specification {

    def "Check arguments processing"() {

        expect: "arg parsing"
        CliUtils.parseArgs('one two   three') as List == ['one', 'two', 'three']
        CliUtils.parseArgs('one "two three"') as List == ['one', '\"two three\"']
        CliUtils.parseArgs('one th"ree') as List == ['one', 'th\"ree']
        CliUtils.parseArgs('one \'two three\' four') as List == ['one', '\'two three\'', 'four']
        CliUtils.parseArgs(['one', 'two', 'three']) as List == ['one', 'two', 'three']
        CliUtils.parseArgs(null) as List == []

        CliUtils.parseArgs('-c exec("import sys;")') as List == ['-c', 'exec("import sys;")']
        CliUtils.parseArgs('-c exec("import sys;print(\"some\")")') as List == ['-c', 'exec("import sys;print(\"some\")")']
    }

    def "Check args merge"() {

        expect: 'merge'
        CliUtils.mergeArgs('one two', null) as List == ['one', 'two']
        CliUtils.mergeArgs('one two', ['three']) as List == ['one', 'two', 'three']
        CliUtils.mergeArgs(null, 'three') as List == ['three']
        CliUtils.mergeArgs(null, null) as List == []
    }

    def "Check output prefixing"() {

        expect: 'output prefixed'
        CliUtils.prefixOutput('sample\noutput', '  ') == "   sample${System.lineSeparator()}   output"
        CliUtils.prefixOutput('sample\noutput', null) == 'sample\noutput'
    }

    def "Check version match check"() {

        expect: 'matched'
        CliUtils.isVersionMatch('2.2.3', null)
        CliUtils.isVersionMatch('2.2.3', '1')
        CliUtils.isVersionMatch('2.2.3', '2')
        CliUtils.isVersionMatch('2.2.3', '2.1')
        CliUtils.isVersionMatch('2.2.3', '2.2')
        CliUtils.isVersionMatch('2.2.3', '2.2.1')
        CliUtils.isVersionMatch('2.2.3', '2.2.3')

        !CliUtils.isVersionMatch('2.2.3', '2.2.4')
        !CliUtils.isVersionMatch('2.2.3', '2.3')
        !CliUtils.isVersionMatch('2.2.3', '3')
        !CliUtils.isVersionMatch('9.0.1', '10.1')
    }

    def "Check bad version format"() {

        when: "bad expected version"
        CliUtils.isVersionMatch('2.2.3', '2.2.2.2')
        then: "error"
        thrown(IllegalArgumentException)
    }

    def "Check command wrapping"() {

        boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)

        expect: 'wrapped'
        CliUtils.wrapCommand('print(\'sample\')') == (isWindows ? 'print(\'sample\')': 'exec("print(\'sample\')")')
        CliUtils.wrapCommand('"print(\'sample\')"') == (isWindows ? '"print(\'sample\')"': 'exec("print(\'sample\')")')
        CliUtils.wrapCommand('exec("print(\'sample\')")') == 'exec("print(\'sample\')")'
        CliUtils.wrapCommand('import sys;print(sys.prefix)') == (isWindows? 'import sys;print(sys.prefix)': 'exec("import sys;print(sys.prefix)")')
        CliUtils.wrapCommand('"import sys;print(sys.prefix+\"smaple\")"') == (isWindows ? '"import sys;print(sys.prefix+"smaple")"' :'exec("import sys;print(sys.prefix+"smaple")")')
    }

    def "Check command line parsing"() {

        println "one \"middle\" two"
        expect:
        CliUtils.parseCommandLine('') as List == []
        CliUtils.parseCommandLine('one two "three four" five') as List == ['one', 'two', '"three four"', 'five']
        CliUtils.parseCommandLine('"one   two"') as List == ['"one   two"']
        CliUtils.parseCommandLine('"one \\"middle\\" two"') as List == ['"one \\"middle\\" two"']
        CliUtils.parseCommandLine('one two\\ three four') as List == ['one', 'two three', 'four']
        CliUtils.parseCommandLine('two\\\\ three') as List == ['two\\ three']
        CliUtils.parseCommandLine('t\\wo three') as List == ['t\\wo', 'three']
        CliUtils.parseCommandLine('two\\') as List == ['two\\']
        CliUtils.parseCommandLine('-c "\'one\', \'two\'"') as List == ['-c', '"\'one\', \'two\'"']
    }

    def "Check wincmd formatting"() {

        setup:
        File home = Files.createTempDir()

        when: "executable not exists"
        CliUtils.wincmdArgs('not_exists', home, [] as String[], false)
        then: "error"
        thrown(ExecException)

        when: "normal executions"
        File ex = new File(home, 'exec')
        ex.createNewFile()
        then: "use absolute path when workDir used"
        CliUtils.wincmdArgs('exec', home, [] as String[], false) as List == ['/c', 'exec']
        CliUtils.wincmdArgs('exec', home, [] as String[], true) as List == ['/c', ex.canonicalPath]

        when: "normal executions"
        File subdir = new File(home, 'exec with space')
        subdir.mkdir()
        ex = new File(subdir, 'exec')
        ex.createNewFile()
        then: "use absolute path when workDir used"
        CliUtils.wincmdArgs('exec', subdir, [] as String[], false) as List == ['/c', 'exec']
        CliUtils.wincmdArgs('exec', subdir, [] as String[], true) as List == ['/c', "\"\"$ex.canonicalPath\"\""]

        cleanup:
        home.deleteDir()
    }
}