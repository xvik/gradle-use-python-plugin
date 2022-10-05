package ru.vyarus.gradle.plugin.python.util

import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 19.12.2017
 */
class DurationFormatTest extends Specification {

    def "Check time formatting"() {

        expect:
        DurationFormatter.format(0) == '0ms'
        DurationFormatter.format(100) == '100ms'
        DurationFormatter.format(1200) == '1.2s'
        DurationFormatter.format(1020) == '1.02s'
        DurationFormatter.format(10_000) == '10s'
        DurationFormatter.format(1_000_000) == '16m 40s'
        DurationFormatter.format(1_000_000_000) == '11d 13h 46m 40s'
        DurationFormatter.format(1*24*60*60*1000) == '1d'
        DurationFormatter.format(1*25*60*60*1000) == '1d 1h'
    }
}
