package ru.vyarus.gradle.plugin.python.util

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
        CliUtils.prefixOutput('sample\noutput', '  ') == '   sample\n   output'
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
    }

    def "Check command wrapping"() {

        expect: 'wrapped'
        CliUtils.wrapCommand('print(\'sample\')') == 'exec("print(\'sample\')")'
        CliUtils.wrapCommand('"print(\'sample\')"') == 'exec("print(\'sample\')")'
        CliUtils.wrapCommand('exec("print(\'sample\')")') == 'exec("print(\'sample\')")'
        CliUtils.wrapCommand('import sys;print(sys.prefix)') == 'exec("import sys;print(sys.prefix)")'
        CliUtils.wrapCommand('"import sys;print(sys.prefix+\"smaple\")"') == 'exec("import sys;print(sys.prefix+"smaple")")'
    }
}