package ru.vyarus.gradle.plugin.python.util

import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 19.12.2017
 */
class DurationFormatTest extends Specification {

    def "Check time formatting"() {

        expect:
        DurationFormatter.format(100) == '0.100s'
        DurationFormatter.format(10_000) == '10.000s'
        DurationFormatter.format(1_000_000) == '16m40.00s'
        DurationFormatter.format(1_000_000_000) == '11d13h46m40.00s'
    }
}
