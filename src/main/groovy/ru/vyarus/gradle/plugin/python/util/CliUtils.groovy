package ru.vyarus.gradle.plugin.python.util

import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.process.internal.ExecException

import java.nio.file.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Cli helper utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
@CompileStatic
final class CliUtils {

    private static final String[] EMPTY = []
    private static final String SPACE = ' '
    private static final String VERSION_SPLIT = '\\.'
    private static final String BACKSLASH = '\\'
    private static final Pattern PIP_CREDENTIALS = Pattern.compile(' --(?>extra-)?index-url +https?://[^:]+:([^@]+)@')

    private CliUtils() {
    }

    /**
     * Searches for binary in system path to reveal incorrect PATH variable. Intended to reveal different PATH
     * in process (might be the case with pyenv).
     *
     * @param binary binary name
     * @return found file
     * @throws GradleException when binary not found
     */
    @SuppressWarnings('NestedForLoop')
    static File searchSystemBinary(String binary) {
        String path = System.getenv('PATH')
        if (path == null || path.empty) {
            throw new GradleException('PATH variable did not contain anything')
        }
        List<String> target = [binary]
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            target.add("${binary}.exe" as String)
        }

        for (final String p : path.split(File.pathSeparator)) {
            for (String name : target) {
                File e = new File(p, name)
                if (e.file) {
                    return e.absoluteFile
                }
            }
        }
        throw new GradleException("'$binary' executable was not found in system. Please check PATH variable " +
                "correctness (current process may not see the same PATH as your shell). \n\t PATH=$path")
    }

    /**
     * Apply prefix for all lines in incoming string.
     *
     * @param output string to apply prefix
     * @param prefix lines prefix
     * @return string with applied prefixes
     */
    static String prefixOutput(String output, String prefix) {
        prefix ? output.readLines().collect { "$prefix $it" }.join(System.lineSeparator()) : output
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
     * Parse arguments from simple string. Support quotes (but not nested).
     * Also, support escaped space ('first\ last' - > argument not split 'first last') and escaped quotes (\\").
     *
     * @param command arguments string
     * @return parsed arguments
     */
    @SuppressWarnings('MethodSize')
    static String[] parseCommandLine(String command) {
        String cmd = command.trim()
        if (cmd) {
            List<String> res = []
            StringBuilder tmp = new StringBuilder()
            // inside quotes (ignore spaces inside quotes), but not escaped quotes
            String quoted
            // \ appear before current char (escape character not printed yet)
            boolean escaped = false
            cmd.each {
                // non escaped quotes char - start/stop quote scope
                if (it in ['"', '\''] && !escaped) {
                    quoted = quoted && it == quoted ? null : (quoted ?: it)
                }
                if (!quoted) {
                    if (it == SPACE) {
                        // ignore multiple split spaces (leading spaces after split)
                        if (tmp.length() == 0) {
                            // ignore leading escaped space
                            escaped = false
                            return
                        }
                        if (escaped) {
                            // for escaped space - do not split, write space only (remove escape)
                            escaped = false
                        } else {
                            // split
                            res << tmp.toString()
                            tmp = new StringBuilder()
                            return
                        }
                    }
                }

                if (escaped) {
                    // write preserved backslash
                    tmp.append(BACKSLASH)
                }
                escaped = it == BACKSLASH

                if (!escaped) {
                    // write current char, if it's not escape (postponed until the next char)
                    tmp.append(it)
                }
            }

            if (escaped) {
                // special case: backslash was the last char
                tmp.append(BACKSLASH)
            }
            // last arg
            res << tmp.toString()
            return res as String[]
        }
        return EMPTY
    }

    /**
     * @param version checked version in format major.minor.micro
     * @param required version constraint (could be null)
     * @return true if version matches requirement (>=)
     */
    static boolean isVersionMatch(String version, String required) {
        boolean valid = true
        if (required) {
            String[] req = required.split(VERSION_SPLIT)
            String[] ver = version.split(VERSION_SPLIT)
            if (req.length > 3) {
                throw new IllegalArgumentException(
                        "Invalid version format: $required. Accepted format: major.minor.micro")
            }
            valid = isPositionMatch(ver, req, 0)
        }
        return valid
    }

    /**
     * Wraps command ('-c print('smth')') into exec() for linux: '-c "print('smth')"' will not be
     * called on linux at all (when used from java).
     *
     * @param command command expression to wrap
     * @param isWindows for windows host
     * @return wrapped expression or original command if its already exec()
     */
    static String wrapCommand(String command, boolean isWindows) {
        if (isWindows || command.startsWith('exec(')) {
            return command
        }
        String cmd = command.replaceAll(/^"|"$/, '')
        return "exec(\"$cmd\")"
    }

    /**
     * Prepare arguments to call python with cmd.
     *
     * @param exec python exec (absolute path)
     * @param projectHome project home directory
     * @param args python arguments
     * @return arguments for cmd
     */
    static String[] wincmdArgs(String executable, File projectHome, String[] args, boolean workDirUsed) {
        // important to resolve relative to project dir
        File file = Paths.get(executable).absolute ? new File(executable) : new File(projectHome, executable)
        // manual check to unify win/linux behaviour
        if (!file.exists()) {
            throw new ExecException("Cannot run program \"$executable\": error=2, No such file or directory")
        }
        // when work dir not used we can use relative path, but with work dir only absolute path
        String exec = workDirUsed ? canonicalPath(file) : executable
        return mergeArgs(['/c', exec.contains(SPACE) ? "\"\"$exec\"\"" : exec], args)
    }

    /**
     * Shortcut for {@link #canonicalPath(java.lang.String, java.lang.String)}.
     *
     * @param file file to get canonical path for
     * @return canonical path for file without following symlinks
     */
    static String canonicalPath(File file) {
        canonicalPath(null, file.absolutePath)
    }

    /**
     * Format canonical path WITHOUT following symlinks (which {@link File#getCanonicalPath()} do). Used to
     * remove redundant ".." parts in path.
     *
     * @param home home dir (may be null if path is absolute)
     * @param file file or directory path
     * @return canonical path (absolute) if target path exists and path as is if not
     */
    static String canonicalPath(String home, String file) {
        String path = file?.trim()
        if (!path) {
            return path
        }
        // important to resolve file relative to project home, because work dir may be gradle daemon root.
        // note: with docker started for windows this will work only with canonical path
        // (even linux path with win separator would be correctly detected on windows)
        Path relative = Paths.get(file)
        if (!relative.absolute && home == null) {
            throw new IllegalArgumentException('Home dir not specified for relative path validation: ' + file)
        }
        Path target = relative.absolute ? relative : new File(home, file).toPath()
        if (Files.exists(target)) {
            try {
                // use absolute path
                return target.toRealPath(LinkOption.NOFOLLOW_LINKS).normalize().toString()
            } catch (AccessDeniedException ignored) {
                // can't correctly access path. NP, using original path instead (hopefully its ok).
                // As an example, this happens for windows python installations from windows store (where
                // prefix points to not accessible path)
                return target
            }
        }
        // return not existing path as is
        return relative.normalize().toString()
    }

    /**
     * @param pythonHome python home path (always absolute)
     * @param windows true to prepare path for windows host
     * @return python binaries path relative to provided python home
     */
    static String pythonBinPath(String pythonHome, boolean windows) {
        // note: with docker started for windows this will work only because python home path was canonicalize
        // (even linux path with win separator would be correctly detected on windows)
        if (!Paths.get(pythonHome).absolute) {
            throw new IllegalArgumentException('Non absolute home path provided: ' + pythonHome)
        }
        canonicalPath(null, windows ? "$pythonHome/Scripts" : "$pythonHome/bin")
    }

    /**
     * Hides credentials from external index urls in pip commands (pip do the same in its output).
     * For example: {@code python -m pip install inner-pkg --extra-index-url http://user:pass@some-url.com}
     * must be printed as {@code [python] -m pip install inner-pkg --extra-index-url http://user:*****@some-url.com}
     *
     * @param cmd python command
     * @return string with cleared passwords
     */
    static String hidePipCredentials(String cmd) {
        if (!cmd.contains(' --extra-index-url ') && !cmd.contains(' --index-url ')) {
            return cmd
        }
        int lastIndex = 0
        StringBuilder output = new StringBuilder()
        Matcher matcher = PIP_CREDENTIALS.matcher(cmd)
        while (matcher.find()) {
            output.append(cmd, lastIndex, matcher.start(1))
                    .append('*****')
                    .append(cmd, matcher.end(1), matcher.end())

            lastIndex = matcher.end()
        }
        if (lastIndex < cmd.length()) {
            output.append(cmd, lastIndex, cmd.length())
        }
        return output.toString()
    }

    /**
     * @return true if host is linux (not mac!), false otherwise
     */
    static boolean isLinuxHost() {
        return Os.isFamily(Os.FAMILY_UNIX) && !Os.isFamily(Os.FAMILY_MAC)
    }

    /**
     * Resolve use home reference (~) in path into correct user directory.
     *
     * @param path path to resolve
     * @return resolved path
     */
    static String resolveHomeReference(String path) {
        String res = path
        if (res.startsWith('~')) {
            res = System.getProperty('user.home') + res.substring(1)
        }
        return res
    }

    private static boolean isPositionMatch(String[] ver, String[] req, int pos) {
        boolean valid = (ver[pos] as Integer) >= (req[pos] as Integer)
        if (valid && ver[pos] == req[pos] && req.length > pos + 1) {
            return isPositionMatch(ver, req, pos + 1)
        }
        return valid
    }
}
