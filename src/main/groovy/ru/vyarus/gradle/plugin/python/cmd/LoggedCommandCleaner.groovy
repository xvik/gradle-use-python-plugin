package ru.vyarus.gradle.plugin.python.cmd

/**
 * Some python commands may require sensitive data which should not be revealed in logs (all executed commands
 * are logged). For example pip may use external index url with auth credentials (in this case password must be
 * hidden).
 * <p>
 * Cleaner must be registered directly into {@link Python} instance with
 * {@link Python#logCommandCleaner(ru.vyarus.gradle.plugin.python.cmd.LoggedCommandCleaner)}.
 * <p>
 * As an example see {@link Pip} constructor which register external index url credentials cleaner into
 * provided python instance. For cleaner implementation see
 * {@link ru.vyarus.gradle.plugin.python.util.CliUtils#hidePipCredentials(java.lang.String)}.
 *
 * @author Vyacheslav Rusakov
 * @since 27.02.2021
 */
interface LoggedCommandCleaner {

    /**
     * Called before logging executed python command into console to hide possible sensitive command parts.
     *
     * @param cmd executed command
     * @return command safe for logging
     */
    String clear(String cmd)
}
