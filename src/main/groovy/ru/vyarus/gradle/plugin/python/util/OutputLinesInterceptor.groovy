package ru.vyarus.gradle.plugin.python.util

import org.apache.tools.ant.util.LineOrientedOutputStream

/**
 * @author Vyacheslav Rusakov
 * @since 16.11.2017
 */
class OutputLinesInterceptor extends LineOrientedOutputStream {

    private Closure action

    OutputLinesInterceptor(Closure action) {
        this.action = action
    }

    @Override
    protected void processLine(String s) throws IOException {
        action.call(s)
    }
}
