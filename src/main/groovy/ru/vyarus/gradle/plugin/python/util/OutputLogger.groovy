package ru.vyarus.gradle.plugin.python.util

import groovy.transform.CompileStatic
import org.apache.tools.ant.util.LineOrientedOutputStream
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

/**
 * Special output stream to be used instead of system.out to redirect output into gradle logger (by line)
 * with prefixing.
 *
 * @author Vyacheslav Rusakov
 * @since 16.11.2017
 */
@CompileStatic
class OutputLogger extends LineOrientedOutputStream {

    private final Logger logger
    private final LogLevel level
    private final String prefix

    OutputLogger(Logger logger, LogLevel level, String prefix) {
        this.logger = logger
        this.level = level
        this.prefix = prefix
    }

    @Override
    protected void processLine(String s) throws IOException {
        String msg = prefix ? "$prefix $s" : s
        logger.log(level, msg)
    }
}
