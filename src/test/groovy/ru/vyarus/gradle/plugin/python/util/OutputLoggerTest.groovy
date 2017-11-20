package ru.vyarus.gradle.plugin.python.util

import org.gradle.api.logging.LogLevel
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 17.11.2017
 */
class OutputLoggerTest extends Specification {

    def "Check output with prefix"() {

        when: "configure logger with prefix"
        def logger = new TestLogger(appendLevel: true)
        new OutputLogger(logger, LogLevel.INFO, '\t').withStream {
            it.write('sample'.getBytes())
        }
        then: "output prefixed"
        logger.res == 'INFO \t sample\n'
    }

    def "Check output without prefix"() {

        when: "configure logger without prefix"
        def logger = new TestLogger(appendLevel: true)
        new OutputLogger(logger, LogLevel.LIFECYCLE, null).withStream {
            it.write('sample'.getBytes())
        }
        then: "output prefixed"
        logger.res == 'LIFECYCLE sample\n'
    }
}