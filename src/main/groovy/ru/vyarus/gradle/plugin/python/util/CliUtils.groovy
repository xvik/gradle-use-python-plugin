package ru.vyarus.gradle.plugin.python.util

/**
 * Cli helper utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
final class CliUtils {

    private static final String[] EMPTY = []
    private static final String SPACE = ' '

    private CliUtils() {
    }

    /**
     * Apply prefix for all lines in incoming string.
     *
     * @param output string to apply prefix
     * @param prefix lines prefix
     * @return string with applied prefixes
     */
    static String prefixOutput(String output, String prefix) {
        prefix ? output.readLines().collect { "$prefix $it" }.join('\n') : output
    }

    /**
     * Merge arguments.
     *
     * @param args1 array, collection or simple string (null allowed)
     * @param args2 array, collection or simple string (null allowed)
     * @return merged collection
     */
    static String[] mergeArgs(Object args1, Object args2) {
        String[] args = []
        args += parseArgs(args1)
        args += parseArgs(args2)
        return args
    }

    /**
     * Parse arguments from multiple formats.
     *
     * @param args array, collection or string
     * @return parsed arguments
     */
    @SuppressWarnings('Instanceof')
    static String[] parseArgs(Object args) {
        String[] res = EMPTY
        if (args) {
            if (args instanceof CharSequence) {
                res = parseCommandLine(args.toString())
            } else {
                res = args as String[]
            }
        }
        return res
    }

    /**
     * Parse arguments from simple string.
     *
     * @param command arguments string
     * @return parsed arguments
     */
    static String[] parseCommandLine(String command) {
        String cmd = command.trim()
        return cmd ? cmd
                .replaceAll('\\s{2,}', SPACE)
                .split(SPACE)
                : EMPTY
    }
}
