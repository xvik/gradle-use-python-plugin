package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecResult
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.DurationFormatter
import ru.vyarus.gradle.plugin.python.util.OutputLogger
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

import java.nio.file.Paths

/**
 * Python commands execution utility. Use global python or binary in provided python path.
 * <p>
 * Usage: configure instance with builder style configuration
 * methods and then execute commands. Command output may be redirected to gradle logger (with configurable
 * level and prefix) or returned as result.
 * <p>
 * Arguments may be defined as:
 * <ul>
 *     <li>array or collection {@code ['arg', 'arg']}</li>
 *     <li>pure string {@code 'arg arg --somekey'}</li>
 * </ul>
 * <p>
 * See {@link Pip} as example of usage for package specific utility.
 *
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
@CompileStatic
@SuppressWarnings(['ConfusingMethodName', 'StaticMethodsBeforeInstanceMethods', 'DuplicateNumberLiteral'])
class Python {

    private static final String PYTHON3 = 'python3'
    private static final String SPACE = ' '

    private final Project project
    private final String executable
    private String workDir
    private String outputPrefix = '\t'
    private LogLevel logLevel = LogLevel.INFO
    private final List<String> pythonArgs = []
    private final List<String> extraArgs = []
    private final Map<String, Object> envVars = [:]

    // run through cmd on win (when direct executable called)
    private final boolean withCmd
    // set when calling custom binary by path instead of global (required to rewrite path to absolute)
    private final boolean customBinaryPath

    // special cleaners for logged commands to hide sensitive data (like passwords)
    private final List<LoggedCommandCleaner> logCleaners = []

    Python(Project project) {
        this(project, null, null)
    }

    Python(Project project, String pythonPath, String binary) {
        this.project = project
        String normalizedPath = pythonPath == null ? null : Paths.get(pythonPath).normalize().toString()
        this.executable = getPythonBinary(project, normalizedPath, binary)
        // direct executable must be called with cmd (https://docs.gradle.org/4.1/dsl/org.gradle.api.tasks.Exec.html)
        this.withCmd = normalizedPath && Os.isFamily(Os.FAMILY_WINDOWS)
        // custom python path used (which may be relative and conflict with workDir)
        this.customBinaryPath = normalizedPath as boolean
    }

    /**
     * @param workDir working directory (null value ignored for simplified usage)
     * @return cli instance for chained calls
     */
    Python workDir(String workDir) {
        if (workDir) {
            this.workDir = workDir
        }
        return this
    }

    /**
     * By default '\t' prefix used (to indent python command output).
     *
     * @param prefix prefix applied to python execution output (null mean no prefix)
     * @return cli instance for chained calls
     */
    Python outputPrefix(String prefix) {
        this.outputPrefix = prefix
        return this
    }

    /**
     * By default, {@link LogLevel#INFO} level is used (visible with gradle '-i' flag).
     * For example, {@link Pip} use {@link LogLevel#LIFECYCLE} because pip logs must be always visible.
     *
     * @param level gradle log level to use for python output (null value ignored for simplified usage)
     * @return cli instance for chained calls
     */
    Python logLevel(LogLevel level) {
        if (level != null) {
            this.logLevel = level
        }
        return this
    }

    /**
     * Python's own args. May be used to apply some python keys to all called commands (like -S or -I).
     * Arguments applied before called command. To clean existing arguments see {@link #clearPythonArgs()}.
     * <p>
     * For example, if we set extra arg '-I' then command call `-m mod something` will become '-I -m mod something'.
     *
     * @param args python arguments (array, collection or simple string) to apply to all processed commands
     * @return cli instance for chained calls
     * @see #extraArgs for applying module-specific args (at the end)
     */
    Python pythonArgs(Object args) {
        if (args) {
            this.pythonArgs.addAll(Arrays.asList(CliUtils.parseArgs(args)))
        }
        return this
    }

    /**
     * Useful if all called commands support common keys (usually this mean one module usage).
     * Arguments are appended. To clean existing arguments see {@link #clearExtraArgs()}.
     * <p>
     * Args applied after command (at the end). Don't use it for python's own args because in many cases they will
     * not work at the end (as would assumed to belonging to called module)!
     * <p>
     * For example, if we set extra arg '-a' then command call `-m mod something` will become '-m mod something -p'.
     *
     * @param args extra arguments (array, collection or simple string) to apply to all processed commands
     * (null value ignored for simplified usage)
     * @return cli instance for chained calls
     * @see #pythonArgs for applying python options
     */
    Python extraArgs(Object args) {
        if (args) {
            this.extraArgs.addAll(Arrays.asList(CliUtils.parseArgs(args)))
        }
        return this
    }

    /**
     * Set environment variable for executed python process.
     * <p>
     * Specified variables could be cleared with {@link #clearEnvironment()}.
     *
     * @param name environment variable
     * @param value variable value
     * @return cli instance for chained calls
     */
    Python environment(String name, Object value) {
        envVars.put(name, value)
        return this
    }

    /**
     * Set environment variables for executed python process. This call does not replace previous variables map,
     * it only adds all specified values into existing map (with possible overriding of some variable values).
     * So if called multiple times, all specified maps would be aggregated.
     * <p>
     * Specified variables could be cleared with {@link #clearEnvironment()}.
     *
     * @param vars environment variables (may be null)
     * @return cli instance for chained calls
     */
    Python environment(Map<String, Object> vars) {
        if (vars) {
            envVars.putAll(vars)
        }
        return this
    }

    /**
     * Registers cleaner for logger command to hide sensitive data (called python command remains
     * unchanged).
     *
     * @param cleaner cleaner to register
     * @return cleared command for log
     */
    Python logCommandCleaner(LoggedCommandCleaner cleaner) {
        logCleaners.add(cleaner)
        return this
    }

    /**
     * Removes all registered python arguments.
     *
     * @return cli instance for chained calls
     */
    Python clearPythonArgs() {
        this.pythonArgs.clear()
        return this
    }

    /**
     * Removes all registered extra arguments.
     *
     * @return cli instance for chained calls
     */
    Python clearExtraArgs() {
        this.extraArgs.clear()
        return this
    }

    Python clearEnvironment() {
        this.envVars.clear()
        return this
    }

    /**
     * Execute python command and return all output (without applying the prefix!).
     * Very handy for short scripts evaluation. In case of error, all output is logged (with prefix).
     *
     * @param args command line arguments (array, collection or simple string)
     * @return python command execution output
     * @throws PythonExecutionFailed when process fails
     */
    @SuppressWarnings('CatchThrowable')
    String readOutput(Object args) {
        return new ByteArrayOutputStream().withStream { os ->
            try {
                processExecution(args, os)
                String output = os.toString().trim()
                project.logger.info(CliUtils.prefixOutput(output, outputPrefix))
                return output
            } catch (Throwable th) {
                // print process output, because it might contain important error details
                String output = os.toString().trim()
                if (output) {
                    project.logger.error(CliUtils.prefixOutput(output, outputPrefix))
                }
                throw th
            }
        }
    }

    /**
     * Execute python command. All output redirected to gradle log (line by line).
     *
     * @param args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void exec(Object args) {
        new OutputLogger(project.logger, logLevel, outputPrefix).withStream { processExecution(args, it) }
    }

    /**
     * Calls command on module. Useful for integrations to avoid manual arguments merge for module.
     *
     * @param module called python module name
     * @param args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void callModule(String module, Object args) {
        exec(CliUtils.mergeArgs("-m $module", args))
    }

    /**
     * Warning: it will return {@code sys.prefix} path, which may not point to actual python installation!
     * For example, on linux this may return "/usr". For virtualenv this will point to environment.
     *
     * @return python home directory (works for global python too)
     */
    @Memoized
    String getHomeDir() {
        return resolveInfo()[1]
    }

    /**
     * By default, working dir should be set to project root on execution
     * (see {@link org.gradle.process.ExecSpec).
     *
     * @return current working directory or null if not defined.
     */
    String getWorkDir() {
        return workDir
    }

    /**
     * Warning: it is NOT based on {@link #getHomeDir()} which may return not actual python installation dir.
     * Binary dir extracted from actual python execution ({@code sys.executable}). Note that {@code sys.executable}
     * MAY return empty string instead and in this case binary path would be guessed from {@link #getHomeDir()}.
     * <p>
     * {@code sysconfig.get_config_var('BINDIR')} also may not be used as virtualenv may be created without
     * setuptools.
     * <p>
     * In case when virtualenv created from another virtualenv, binary dir will return correct path, but
     * {@link #getHomeDir()} most likely will point to global python.
     *
     * @return directory under python home containing python binary (always absolute path)
     */
    @Memoized
    String getBinaryDir() {
        // use resolved executable to avoid incorrect resolution in case of venv inside venv
        String path = customBinaryPath ? project.file(executable).absolutePath : resolveInfo()[2]?.trim()
        int idx = path.lastIndexOf(File.separator)

        if (path.empty || idx <= 0) {
            // just guess by home dir (yes, I know, this MIGHT be incorrect in some cases, but should be ok
            // for virtualenvs used in majority of cases)
            path = homeDir
            return CliUtils.pythonBinPath(path)
        }

        // cut off binary
        return path[0..idx - 1]
    }

    /**
     * @return python version in format major.minor.micro
     */
    @Memoized
    String getVersion() {
        return resolveInfo()[0]
    }

    /**
     * Checks if current environment is a virtualenv. It is impossible to use {@code sys.base_path},
     * {@code sys.real_path} or even {@code os.getenv('VIRTUAL_ENV')} for detection because they might be not set
     * even under execution within virtual environment. Instead, checked presence of "activation" script inside
     * python installation binary path: for virtual environment such script would be present.
     * <p>
     * This should be able to detect not only virtualenv environment but also venv (and maybe other also using
     * activation file).
     *
     * @return true if used python is a virtualenv, false for normal python installation
     */
    @Memoized
    boolean isVirtualenv() {
        // always absolute path
        String activationScript = binaryDir + File.separator + 'activate'
        return new File(activationScript).exists()
    }

    /**
     * Used to call python instance methods with hidden (not visible in gradle console) log (set to INFO).
     *
     * @param closure closure with python commands to execute
     */
    public <T> T withHiddenLog(Closure closure) {
        LogLevel level = logLevel
        logLevel(LogLevel.INFO)
        try {
            return (T) closure.call()
        } finally {
            logLevel(level)
        }
    }

    /**
     * Note: in most cases it would be just "python" (or "python.exe"), assuming resolution through global PATH.
     * If python path was manually configured then complete path would be returned. For virtualenv, path
     * to virtual environment would be returned.
     *
     * @return python binary used
     */
    String getUsedBinary() {
        this.executable
    }

    @SuppressWarnings('UnnecessarySetter')
    @CompileStatic(TypeCheckingMode.SKIP)
    private void processExecution(Object args, OutputStream os) {
        boolean wrkDirUsed = workDir as boolean
        // on win non global python could be called only through cmd
        String exec = withCmd ? 'cmd'
                // use absolute python path if work dir set (relative will simply not work)
                : (wrkDirUsed && customBinaryPath ? CliUtils.canonicalPath(project.file(executable)) : executable)
        String[] cmd = withCmd ?
                CliUtils.wincmdArgs(executable, project.projectDir, prepareArgs(args), wrkDirUsed)
                : prepareArgs(args)

        // important to show arguments as array to easily spot args parsing problems
        project.logger.info('Python arguments: {}', cmd)

        String commandLine = "$exec ${cmd.join(SPACE)}"
        String formattedCommand = cleanLoggedCommand(
                commandLine.replace('\r', '').replace('\n', SPACE))
        project.logger.log(logLevel, "[python] $formattedCommand")

        long start = System.currentTimeMillis()
        ExecResult res = project.exec {
            it.executable = exec
            it.args(cmd)
            standardOutput = os
            errorOutput = os
            ignoreExitValue = true
            if (workDir) {
                setWorkingDir(workDir)
            }
            if (envVars) {
                it.environment(envVars)
            }
        }
        project.logger.info('Python execution time: {}',
                DurationFormatter.format(System.currentTimeMillis() - start))
        if (res.exitValue != 0) {
            throw new PythonExecutionFailed("Python call failed: $formattedCommand")
        }
    }

    @Memoized
    @CompileStatic(TypeCheckingMode.SKIP)
    private static String getPythonBinary(Project project, String pythonPath, String binary) {
        String res = binary ?: 'python'
        boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
        if (pythonPath) {
            String path = pythonPath + (pythonPath.endsWith(File.separator) ? '' : File.separator)
            // $pythonPath/$binaryName(.exe)
            res = isWindows ? "${path}${res}.exe" : "$path$res"
        } else if (!binary && !isWindows) {
            // check if python3 available
            new ByteArrayOutputStream().withStream { os ->
                ExecResult ret = project.exec {
                    standardOutput = os
                    errorOutput = os
                    ignoreExitValue = true
                    commandLine PYTHON3, '--version'
                }
                if (ret.exitValue == 0) {
                    res = PYTHON3
                }
            }
        }
        return res
    }

    @SuppressWarnings('Instanceof')
    private String[] prepareArgs(Object args) {
        String[] cmd = CliUtils.parseArgs(args)
        detectAndWrapCommand(cmd)
        if (this.pythonArgs) {
            cmd = CliUtils.mergeArgs(pythonArgs, cmd)
        }
        if (this.extraArgs) {
            cmd = CliUtils.mergeArgs(cmd, extraArgs)
        }
        return cmd
    }

    /**
     * Detect python command call (-c) and wrap command argument if required (on linux).
     * @param cmd command to execute
     */
    private void detectAndWrapCommand(String[] cmd) {
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

    /**
     * Reduce the number of python executions during initialization.
     *
     * @return [raw python version, python home path, python executable]
     */
    @Memoized
    private List<String> resolveInfo() {
        String[] cmd = [
                'ver=sys.version_info',
                'print(str(ver.major)+\'.\'+str(ver.minor)+\'.\'+str(ver.micro))',
                'print(sys.prefix)',
                'print(sys.executable)',
        ]
        withHiddenLog {
            // raw version, home path, executable
            List<String> res = readScriptOutput(cmd)
            // remove possible relative section from path (/dd/dd/../tt -> /dd/tt)
            // without following symlinks (very important!)
            res.set(1, CliUtils.canonicalPath(project.rootDir.absolutePath, res.get(1)))
            res.set(2, CliUtils.canonicalPath(project.rootDir.absolutePath, res.get(2)))

            return res
        }
    }

    private List<String> readScriptOutput(String... lines) {
        List<String> cmd = []
        cmd.add('import sys')
        cmd.addAll(lines)

        // IMPORTANT -S should not be used here as it affects behaviour a lot (even sys.prefix may be different)
        return readOutput("-c \"${cmd.join(';')}\"").readLines()
    }

    private String cleanLoggedCommand(String cmd) {
        String res = cmd
        logCleaners.each { res = it.clear(cmd) }
        return res
    }
}
