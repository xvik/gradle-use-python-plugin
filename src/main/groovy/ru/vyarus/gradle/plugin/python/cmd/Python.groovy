package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import ru.vyarus.gradle.plugin.python.cmd.docker.DockerConfig
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.cmd.exec.PythonBinary
import ru.vyarus.gradle.plugin.python.util.CliUtils
import ru.vyarus.gradle.plugin.python.util.OutputLogger
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

import java.util.function.Supplier

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
@SuppressWarnings(['ConfusingMethodName', 'StaticMethodsBeforeInstanceMethods',
        'DuplicateNumberLiteral', 'MethodCount', 'ClassSize'])
class Python {

    private final Environment environment
    private final PythonBinary binary

    private String outputPrefix = '\t'
    private LogLevel logLevel = LogLevel.INFO
    private final List<String> pythonArgs = []
    private final List<String> extraArgs = []

    Python(Environment environment) {
        this(environment, null, null)
    }

    Python(Environment environment, String pythonPath, String binary) {
        this.environment = environment
        this.binary = new PythonBinary(environment, pythonPath, binary)
    }

    /**
     * System binary search is performed only for global python (when pythonPath is not specified). Enabled by default.
     *
     * @param validate true to search python binary in system path and fail if not found
     * @return cli instance for chained calls
     */
    Python validateSystemBinary(boolean validate) {
        this.binary.validateSystemBinary(validate)
        return this
    }

    /**
     * Enable docker support: all python commands would be executed under docker container.
     *
     * @param docker docker configuration (may be null)
     * @return cli instance for chained calls
     */
    Python withDocker(DockerConfig docker) {
        this.binary.withDocker(docker)
        return this
    }

    /**
     * @param workDir working directory (null value ignored for simplified usage)
     * @return cli instance for chained calls
     */
    Python workDir(String workDir) {
        if (workDir) {
            binary.workDir(workDir)
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
     * For example, if we set extra arg '-a' then command call `-m mod something` will become '-m mod something -a'.
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
        binary.envVars.put(name, value)
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
            binary.envVars.putAll(vars)
        }
        return this
    }

    /**
     * Perform pre-initialization and, if required, validate global python binary correctness. Calling this method is
     * NOT REQUIRED: initialization will be performed automatically before first execution. But it might be called
     * in order to throw possible initialization error before some other logic (related to exception handling).
     *
     * @return cli instance for chained calls
     */
    Python validate() {
        binary.init()
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
        binary.addLogCleaner(cleaner)
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
        binary.envVars.clear()
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
                environment.logger.info(CliUtils.prefixOutput(output, outputPrefix))
                return output
            } catch (Throwable th) {
                // print process output, because it might contain important error details
                String output = os.toString().trim()
                if (output) {
                    environment.logger.error(CliUtils.prefixOutput(output, outputPrefix))
                }
                throw th
            }
        }
    }

    /**
     * Execute python command. All output redirected to gradle log (line by line).
     * <p>
     * Shortcut for {@link #exec(org.gradle.api.logging.Logger, java.lang.Object)} with project logger.
     *
     * @param args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void exec(Object args) {
        exec(environment.logger, args)
    }

    /**
     * Execute python command. All output redirected to gradle log (line by line).
     * <p>
     * Custom logger is required ONLY for cases when log messages might come from different thread
     * (like with exclusive docker execution) - in this case project logger might log near previous task (but direct
     * task logger usage workarounds this problem).
     *
     * @param logger logger to use
     * @param args args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void exec(Logger logger, Object args) {
        new OutputLogger(logger, logLevel, outputPrefix).withStream { processExecution(args, it) }
    }

    /**
     * Calls command on module. Useful for integrations to avoid manual arguments merge for module.
     * <p>
     * Shortcut for {@link #callModule(org.gradle.api.logging.Logger, java.lang.String, java.lang.Object)} with
     * project logger.
     *
     * @param module called python module name
     * @param args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void callModule(String module, Object args) {
        exec(CliUtils.mergeArgs("-m $module", args))
    }

    /**
     * Calls command on module. Useful for integrations to avoid manual arguments merge for module.
     * <p>
     * Custom logger is required ONLY for cases when log messages might come from different thread
     * (like with exclusive docker execution) - in this case project logger might log near previous task (but direct
     * task logger usage workarounds this problem).
     *
     * @param logger logger to use
     * @param module called python module name
     * @param args command line arguments (array, collection or simple string)
     * @throws PythonExecutionFailed when process fails
     */
    void callModule(Logger logger, String module, Object args) {
        exec(logger, CliUtils.mergeArgs("-m $module", args))
    }

    /**
     * @param module module name
     * @return true if module exists, false otherwise
     * @see <a href="https://docs.python.org/2.7/library/imp.html#imp.find_module">old find_module (2.x)</a>
     * @see <a href="https://docs.python.org/3/library/importlib.html#importlib.find_loader">find_loader</a>
     * @see <a href="https://docs.python.org/3/library/importlib.html#importlib.util.find_spec">find_spec</a>
     */
    @SuppressWarnings('BlockEndsWithBlankLine')
    boolean isModuleExists(String module) {
        String pythonVersion = version
        List<String> res
        if (CliUtils.isVersionMatch(pythonVersion, '3.4')) {
            res = readScriptOutput('import importlib.util',
                    "print(importlib.util.find_spec('$module') is not None)")

        } else if (CliUtils.isVersionMatch(pythonVersion, '3.0')) {
            res = readScriptOutput('import importlib',
                    "print(importlib.find_loader('$module') is not None)")

        } else {
            // python 2, submodules does not supported! (e.g. "mod.sub")
            res = readScriptOutput('import imp',
                    'try:',
                    "    imp.find_module('$module')",
                    '    print(\'True\')',
                    'except ImportError:',
                    '    print(\'False\')')
        }
        return res[0].toLowerCase() == 'true'
    }

    /**
     * This is important for docker because windows host could call linux container and so host os must be ignored.
     *
     * @return true if target os is windows, false otherwise
     */
    boolean isWindows() {
        return binary.windows
    }

    /**
     * Warning: it will return {@code sys.prefix} path, which may not point to actual python installation!
     * For example, on linux this may return "/usr". For virtualenv this will point to environment.
     *
     * @return python home directory (works for global python too)
     */
    String getHomeDir() {
        return resolveInfo()[1]
    }

    /**
     * Same as {@link #getHomeDir()} but with separators according to target docker os (no difference if docker not
     * used). Difference might appear for example when windows host used for linux container.
     * <p>
     * Suitable for uniform output or unified python paths comparisons.
     * <p>
     * Note: change could not be applied to the original method because host-specific separators are important for
     * some logic paths.
     *
     * @return homeDir path with separators aligned with docker os (if docker used)
     */
    String getCanonicalHomeDir() {
        return binary.targetOsCanonicalPath(homeDir)
    }

    /**
     * By default, working dir should be set to project root on execution
     * (see {@link org.gradle.process.ExecSpec).
     *
     * @return current working directory or null if not defined.
     */
    String getWorkDir() {
        return binary.workDir
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
    @SuppressWarnings('ClosureAsLastMethodParameter')
    String getBinaryDir() {
        return getOrCompute("binary.dir:${binary.identity}") {
            // sys.executable and sys.prefix
            return binary.getBinaryDir({ resolveInfo()[2]?.trim() }, { homeDir })
        } as String
    }

    /**
     * Same as {@link #getBinaryDir()} but with separators according to target docker os (no difference if docker not
     * used). Difference might appear for example when windows host used for linux container.
     * <p>
     * Suitable for uniform output or unified python paths comparisons.
     * <p>
     * Note: change could not be applied to the original method because host-specific separators are important for
     * some logic paths.
     *
     * @return binaryDir path with separators aligned with docker os (if docker used)
     */
    String getCanonicalBinaryDir() {
        return binary.targetOsCanonicalPath(binaryDir)
    }

    /**
     * @return python version in format major.minor.micro
     */
    String getVersion() {
        return resolveInfo()[0]
    }

    /**
     * Checks if current environment is a virtualenv (or venv). It is impossible to use {@code sys.base_path},
     * {@code sys.real_path} or even {@code os.getenv('VIRTUAL_ENV')} for detection because they might be not set
     * even under execution within virtual environment. Instead, checked presence of "activation" script inside
     * python installation binary path: for virtual environment such script would be present.
     * <p>
     * This should be able to detect not only virtualenv environment but also venv (and maybe other also using
     * activation file).
     *
     * @return true if used python is a virtualenv, false for normal python installation
     */
    boolean isVirtualenv() {
        return getOrCompute('python.is.virtualenv') {
            // always absolute path
            String path = binaryDir + File.separator + 'activate'
            return binary.exists(path)
        } as Boolean
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
        this.binary.executable
    }

    /**
     * Same as {@link #getUsedBinary()} but with separators according to target docker os (no difference if docker not
     * used). Difference might appear for example when windows host used for linux container.
     * <p>
     * Suitable for uniform output or unified python paths comparisons.
     * <p>
     * Note: change could not be applied to the original method because host-specific separators are important for
     * some logic paths.
     *
     * @return binary path with separators aligned with docker os (if docker used)
     */
    String getCanonicalUsedBinary() {
        binary.targetOsCanonicalPath(usedBinary)
    }

    /**
     * Shortcut to cache values related to the same python (not the same instance, but the same python, related
     * to current gradle project).
     * <p>
     * Project-specific map used for caching (to unify cache for all python instances, created per task).
     * Actual cache key also includes python path to avoid clashes when multiple pythons used.
     *
     * @param key cache key
     * @param value value computation action
     * @return cached or computed value
     */
    public <T> T getOrCompute(String key, Supplier<T> value) {
        return environment.projectCache(key + ':' + binary.identity, value)
    }

    @Override
    String toString() {
        return "$canonicalHomeDir (python $version)"
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void processExecution(Object args, OutputStream os) {
        binary.exec(args, pythonArgs, extraArgs, os, logLevel)
    }

    /**
     * Reduce the number of python executions during initialization.
     *
     * @return [raw python version, python home path, python executable]
     */
    private List<String> resolveInfo() {
        return (List<String>) getOrCompute('python.info') {
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
                res.set(1, CliUtils.canonicalPath(environment.rootDir.absolutePath, res.get(1)))
                res.set(2, CliUtils.canonicalPath(environment.rootDir.absolutePath, res.get(2)))

                return res
            }
        }
    }

    private List<String> readScriptOutput(String... lines) {
        List<String> cmd = []
        cmd.add('import sys')
        cmd.addAll(lines)
        // IMPORTANT -S should not be used here as it affects behaviour a lot (even sys.prefix may be different)
        return readOutput("-c \"${cmd.join(';')}\"").readLines()
    }
}
