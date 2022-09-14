package ru.vyarus.gradle.plugin.python.util

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import ru.vyarus.gradle.plugin.python.cmd.LoggedCommandCleaner

import javax.annotation.Nullable
import java.nio.file.Paths

/**
 * Abstraction for python binary paths logic. Separated from {@link ru.vyarus.gradle.plugin.python.cmd.Python} to
 * encapsulate system-specific python binary search and python command formatting logic.
 * <p>
 * Intentionally does not include python execution logic (only collects os-specific hacks).
 *
 * @author Vyacheslav Rusakov
 * @since 30.08.2022
 */
@CompileStatic
final class PythonBinary {

    private static final String PROP_PYTHON3 = 'ru.vyarus.python3.detected'
    private static final String PYTHON3 = 'python3'

    private static final String SPACE = ' '
    private static final String NL = '\n'

    private static final Object SYNC = new Object()

    private final Project project
    private final String executable
    // run through cmd on win (when direct executable called)
    private final boolean withCmd
    // set when calling custom binary by path instead of global (required to rewrite path to absolute)
    private final boolean customBinaryPath

    // special cleaners for logged commands to hide sensitive data (like passwords)
    private final List<LoggedCommandCleaner> logCleaners = []

    PythonBinary(Project project, String pythonPath, String binary, boolean validateSystemBinary) {
        this.project = project
        this.executable = findPython(project, pythonPath, binary, validateSystemBinary)
        // direct executable must be called with cmd (https://docs.gradle.org/4.1/dsl/org.gradle.api.tasks.Exec.html)
        this.withCmd = pythonPath && Os.isFamily(Os.FAMILY_WINDOWS)
        // custom python path used (which may be relative and conflict with workDir)
        this.customBinaryPath = pythonPath as boolean
    }

    void addLogCleaner(LoggedCommandCleaner cleaner) {
        logCleaners.add(cleaner)
    }

    String getExecutable() {
        executable
    }

    /**
     * @param sysExecutableProvider sys.executable value provider
     * @param sysPrefixProvider sys.prefix value provider
     * @return directory under python home containing python binary (always absolute path)
     * @see {@link ru.vyarus.gradle.plugin.python.cmd.Python#getBinaryDir()}
     */
    String getBinaryDir(Closure<String> sysExecutableProvider, Closure<String> sysPrefixProvider) {
        // use resolved executable to avoid incorrect resolution in case of venv inside venv
        String path = customBinaryPath ? project.file(executable).absolutePath : sysExecutableProvider.call()
        int idx = path.lastIndexOf(File.separator)

        if (path.empty || idx <= 0) {
            // just guess by home dir (yes, I know, this MIGHT be incorrect in some cases, but should be ok
            // for virtualenvs used in majority of cases)
            path = sysPrefixProvider.call()
            return CliUtils.pythonBinPath(path)
        }

        // cut off binary
        return path[0..idx - 1]
    }

    @SuppressWarnings('ParameterCount')
    void exec(Object args,
              List<String> pythonArgs,
              List<String> extraArgs,
              OutputStream outputConsumer,
              String workDir,
              Map<String, Object> environment,
              LogLevel logLevel) {
        String exec = getCommandBinary(workDir)
        String[] cmdArgs = getCommandArguments(workDir, args, pythonArgs, extraArgs)

        // important to show arguments as array to easily spot args parsing problems
        project.logger.info('Python arguments: {}', cmdArgs)

        String[] cmd = [exec]
        cmd += cmdArgs

        String commandLine = cmd.join(SPACE)
        String formattedCommand = cleanLoggedCommand(commandLine.replace('\r', '').replace(NL, SPACE))
        project.logger.log(logLevel, "[python] $formattedCommand")

        long start = System.currentTimeMillis()
        int res = rawExec(cmd, outputConsumer, workDir, environment)
        project.logger.info('Python execution time: {}',
                DurationFormatter.format(System.currentTimeMillis() - start))
        if (res != 0) {
            // duplicating output in error message to be sure it would be visible
            String out = reformatOutputForException(outputConsumer)
            throw new PythonExecutionFailed("Python call failed: $formattedCommand"
                    + (out ? '\n\n\tOUTPUT:\n' + out : ''))
        }
    }

    @SuppressWarnings('Instanceof')
    private static String[] prepareArgs(Object args, List<String> pythonArgs, List<String> extraArgs) {
        String[] cmd = CliUtils.parseArgs(args)
        detectAndWrapCommand(cmd)
        if (pythonArgs) {
            cmd = CliUtils.mergeArgs(pythonArgs, cmd)
        }
        if (extraArgs) {
            cmd = CliUtils.mergeArgs(cmd, extraArgs)
        }
        return cmd
    }

    /**
     * Detect python command call (-c) and wrap command argument if required (on linux).
     *
     * @param cmd command to execute
     */
    private static void detectAndWrapCommand(String[] cmd) {
        boolean moduleCall = false
        cmd.eachWithIndex { String arg, int i ->
            if (arg == '-m') {
                moduleCall = true
            }
            if (!moduleCall && arg == '-c' && i + 2 == cmd.length) {
                // wrap command to grant cross-platform compatibility (simple -c "string" is not always executed)
                cmd[i + 1] = CliUtils.wrapCommand(cmd[i + 1])
            }
        }
    }

    // note: detectPython3Binary and findSystemBinary where extracted from this method to avoid holding
    // Project instance as cache key.
    @Memoized
    private static String getPythonBinary(String pythonPath, String binary, boolean python3Available) {
        String res = binary ?: 'python'
        boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
        if (pythonPath) {
            String path = pythonPath + (pythonPath.endsWith(File.separator) ? '' : File.separator)
            // $pythonPath/$binaryName(.exe)
            res = isWindows ? "${path}${res}.exe" : "$path$res"
        } else if (!binary && python3Available) {
            res = PYTHON3
        }
        return res
    }

    // cached (statically) to prevent multiple fs lookups because multiple Python objects would be constructed
    @Memoized
    private static File findSystemBinary(String binary) {
        // manually searching in system path to point to possibly incorrect PATH variable used in process
        // (might happen when process not started from user shell)
        return CliUtils.searchSystemBinary(binary)
    }

    private String findPython(Project project, String pythonPath, String binary, boolean validateSystemBinary) {
        String normalizedPath = pythonPath == null ? null : Paths.get(pythonPath).normalize().toString()
        // detecting python3 binary only if default python usage assumed (to avoid redundant python calls)
        boolean python3found = (binary || pythonPath) ? false : detectPython3Binary(project)
        String executable = getPythonBinary(normalizedPath, binary, python3found)
        if (validateSystemBinary && !pythonPath) {
            // search would fail if no binary found in system paths
            project.logger.info('Found python binary: {}', findSystemBinary(executable).absolutePath)
        }
        return executable
    }

    // note: @Memoized not used because it would store link to Project object which could lead to significant
    // memory leak. And that's why this check is performed outside of getPythonBinary method
    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('SynchronizedMethod')
    private boolean detectPython3Binary(Project project) {
        synchronized (SYNC) {
            // root project property used for cache execution result in multi-module project
            if (project.rootProject.findProperty(PROP_PYTHON3) == null) {
                if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                    project.rootProject.extensions.extraProperties.set(PROP_PYTHON3, false)
                } else {
                    new ByteArrayOutputStream().withStream { os ->
                        String[] cmd = [PYTHON3, '--version']
                        project.rootProject.extensions.extraProperties.set(PROP_PYTHON3, rawExec(cmd, os) == 0)
                    }
                }
            }
            return project.rootProject.findProperty(PROP_PYTHON3)
        }
    }

    private String cleanLoggedCommand(String cmd) {
        String res = cmd
        logCleaners.each { res = it.clear(cmd) }
        return res
    }

    private String reformatOutputForException(OutputStream os) {
        os.flush()
        String out = os
        return out ? out.split(NL).collect { '\t\t' + it }.join(NL) + NL : ''
    }

    // IMPORTANT: single point of execution for all python commands
    @SuppressWarnings('UnnecessarySetter')
    private int rawExec(String[] command,
                        OutputStream out,
                        @Nullable String workDir = null,
                        @Nullable Map<String, Object> environment = null) {
        ExecResult res = project.exec new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                spec.commandLine command
                spec.standardOutput = out
                spec.errorOutput = out
                spec.ignoreExitValue = true
                if (workDir) {
                    spec.setWorkingDir(workDir)
                }
                if (environment) {
                    spec.environment(environment)
                }
            }
        }
        return res.exitValue
    }

    /**
     * @param workDir work directory
     * @return execution binary (wrapped, if required for correct execution)
     */
    private String getCommandBinary(String workDir) {
        boolean wrkDirUsed = workDir as boolean
        // on win non global python could be called only through cmd
        return withCmd ? 'cmd'
                // use absolute python path if work dir set (relative will simply not work)
                : (wrkDirUsed && customBinaryPath ? CliUtils.canonicalPath(project.file(executable)) : executable)
    }

    /**
     * Additionally, wraps python command call (-c) to properly execute on linux.
     *
     * @param workDir work dir
     * @param args command
     * @return formatted arguments
     */
    private String[] getCommandArguments(String workDir, Object args, List<String> pythonArgs, List<String> extraArgs) {
        boolean wrkDirUsed = workDir as boolean
        return withCmd ? CliUtils
                .wincmdArgs(executable, project.projectDir, prepareArgs(args, pythonArgs, extraArgs), wrkDirUsed)
                : prepareArgs(args, pythonArgs, extraArgs)
    }
}
