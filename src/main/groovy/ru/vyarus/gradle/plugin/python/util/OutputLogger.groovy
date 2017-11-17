package ru.vyarus.gradle.plugin.python.util

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
class OutputLogger extends LineOrientedOutputStream {

    private final Logger logger
    private LogLevel level
    private final String prefix

    OutputLogger(Logger logger, LogLevel level, String prefix) {
        this.logger = logger
        this.level = level
        this.prefix = prefix
    }

    @Override
    protected void processLine(String s) throws IOException {
        logger.log(level, prefix ? "$prefix $s" : s)
    }
}
