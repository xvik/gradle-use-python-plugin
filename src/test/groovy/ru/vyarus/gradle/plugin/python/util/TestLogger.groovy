package ru.vyarus.gradle.plugin.python.util

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.slf4j.Marker

/**
 * @author Vyacheslav Rusakov
 * @since 17.11.2017
 */
class TestLogger implements Logger {

    StringBuilder output = new StringBuilder()
    boolean appendLevel

    void reset() {
        output = new StringBuilder()
    }

    String getRes() {
        return  output.toString()
    }

    private void line(LogLevel level, String msg) {
        if (appendLevel) {
            output.append(level.name()).append(' ')
        }
        output.append(msg).append('\n')
    }

    @Override
    boolean isLifecycleEnabled() {
        return false
    }

    @Override
    void debug(String message, Object... objects) {

    }

    @Override
    void lifecycle(String message) {
        line(LogLevel.LIFECYCLE, message)
    }

    @Override
    void lifecycle(String message, Object... objects) {
        line(LogLevel.LIFECYCLE, message)
    }

    @Override
    void lifecycle(String message, Throwable throwable) {
        line(LogLevel.LIFECYCLE, message)
    }

    @Override
    boolean isQuietEnabled() {
        return false
    }

    @Override
    void quiet(String message) {

    }

    @Override
    void quiet(String message, Object... objects) {

    }

    @Override
    void info(String message, Object... objects) {

    }

    @Override
    void quiet(String message, Throwable throwable) {

    }

    @Override
    boolean isEnabled(LogLevel level) {
        return false
    }

    @Override
    void log(LogLevel level, String message) {
        line(level, message)
    }

    @Override
    void log(LogLevel level, String message, Object... objects) {
        line(level, message)
    }

    @Override
    void log(LogLevel level, String message, Throwable throwable) {
        line(level, message)
    }

    @Override
    String getName() {
        return null
    }

    @Override
    boolean isTraceEnabled() {
        return false
    }

    @Override
    void trace(String s) {

    }

    @Override
    void trace(String s, Object o) {

    }

    @Override
    void trace(String s, Object o, Object o1) {

    }

    @Override
    void trace(String s, Object... objects) {

    }

    @Override
    void trace(String s, Throwable throwable) {

    }

    @Override
    boolean isTraceEnabled(Marker marker) {
        return false
    }

    @Override
    void trace(Marker marker, String s) {

    }

    @Override
    void trace(Marker marker, String s, Object o) {

    }

    @Override
    void trace(Marker marker, String s, Object o, Object o1) {

    }

    @Override
    void trace(Marker marker, String s, Object... objects) {

    }

    @Override
    void trace(Marker marker, String s, Throwable throwable) {

    }

    @Override
    boolean isDebugEnabled() {
        return false
    }

    @Override
    void debug(String s) {

    }

    @Override
    void debug(String s, Object o) {

    }

    @Override
    void debug(String s, Object o, Object o1) {

    }

    @Override
    void debug(String s, Throwable throwable) {

    }

    @Override
    boolean isDebugEnabled(Marker marker) {
        return false
    }

    @Override
    void debug(Marker marker, String s) {

    }

    @Override
    void debug(Marker marker, String s, Object o) {

    }

    @Override
    void debug(Marker marker, String s, Object o, Object o1) {

    }

    @Override
    void debug(Marker marker, String s, Object... objects) {

    }

    @Override
    void debug(Marker marker, String s, Throwable throwable) {

    }

    @Override
    boolean isInfoEnabled() {
        return false
    }

    @Override
    void info(String s) {

    }

    @Override
    void info(String s, Object o) {

    }

    @Override
    void info(String s, Object o, Object o1) {

    }

    @Override
    void info(String s, Throwable throwable) {

    }

    @Override
    boolean isInfoEnabled(Marker marker) {
        return false
    }

    @Override
    void info(Marker marker, String s) {

    }

    @Override
    void info(Marker marker, String s, Object o) {

    }

    @Override
    void info(Marker marker, String s, Object o, Object o1) {

    }

    @Override
    void info(Marker marker, String s, Object... objects) {

    }

    @Override
    void info(Marker marker, String s, Throwable throwable) {

    }

    @Override
    boolean isWarnEnabled() {
        return false
    }

    @Override
    void warn(String s) {

    }

    @Override
    void warn(String s, Object o) {

    }

    @Override
    void warn(String s, Object... objects) {

    }

    @Override
    void warn(String s, Object o, Object o1) {

    }

    @Override
    void warn(String s, Throwable throwable) {

    }

    @Override
    boolean isWarnEnabled(Marker marker) {
        return false
    }

    @Override
    void warn(Marker marker, String s) {

    }

    @Override
    void warn(Marker marker, String s, Object o) {

    }

    @Override
    void warn(Marker marker, String s, Object o, Object o1) {

    }

    @Override
    void warn(Marker marker, String s, Object... objects) {

    }

    @Override
    void warn(Marker marker, String s, Throwable throwable) {

    }

    @Override
    boolean isErrorEnabled() {
        return false
    }

    @Override
    void error(String s) {

    }

    @Override
    void error(String s, Object o) {

    }

    @Override
    void error(String s, Object o, Object o1) {

    }

    @Override
    void error(String s, Object... objects) {

    }

    @Override
    void error(String s, Throwable throwable) {

    }

    @Override
    boolean isErrorEnabled(Marker marker) {
        return false
    }

    @Override
    void error(Marker marker, String s) {

    }

    @Override
    void error(Marker marker, String s, Object o) {

    }

    @Override
    void error(Marker marker, String s, Object o, Object o1) {

    }

    @Override
    void error(Marker marker, String s, Object... objects) {

    }

    @Override
    void error(Marker marker, String s, Throwable throwable) {

    }
}
