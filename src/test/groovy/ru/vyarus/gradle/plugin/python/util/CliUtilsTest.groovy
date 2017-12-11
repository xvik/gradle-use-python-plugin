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
        CliUtils.parseArgs('one \'two three\' four') as List == ['one', '\'two three\'', 'four']
        CliUtils.parseArgs(['one', 'two', 'three']) as List == ['one', 'two', 'three']
        CliUtils.parseArgs(null) as List == []
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
}