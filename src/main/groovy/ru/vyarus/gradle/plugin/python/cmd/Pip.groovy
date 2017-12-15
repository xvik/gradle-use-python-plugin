package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import ru.vyarus.gradle.plugin.python.util.PythonExecutionFailed

/**
 * Pip commands execution utility. Use {@link Python} internally.
 *
 * @author Vyacheslav Rusakov
 * @since 15.11.2017
 */
@CompileStatic
class Pip {

    public static final String USER = '--user'
    private static final List<String> USER_AWARE_COMMANDS = ['install', 'list', 'freeze']

    private final Python python
    final boolean userScope

    Pip(Project project) {
        this(project, null, null, true)
    }

    Pip(Project project, String pythonPath, String binary, boolean userScope) {
        python = new Python(project, pythonPath, binary)
                .logLevel(LogLevel.LIFECYCLE)
        this.userScope = userScope
    }

    /**
     * Install module.
     *
     * @param module module name with version (e.g. 'some==12.3')
     */
    void install(String module) {
        exec("install $module")
    }

    /**
     * Uninstall module.
     *
     * @param module module name
     */
    void uninstall(String module) {
        exec("uninstall $module -y")
        if (isInstalled(module)) {
            // known problem
            throw new GradleException("Failed to uninstall module $module. Try to update pip: " +
                    '\'pip install -U pip\' or try to manually remove package ' +
                    '(probably not enough permissions)')
        }
    }

    /**
     * @param module module to check
     * @return true if module installed
     */
    boolean isInstalled(String module) {
        try {
            // has no output on error, so nothing will appear in log
            readOutput("show $module")
        } catch (PythonExecutionFailed ignored) {
            return false
        }
        return true
    }

    /**
     * Execute command on pip module. E.g. 'install some==12.3'.
     *
     * @param cmd pip command to execute
     */
    void exec(String cmd) {
        python.callModule('pip', applyUserFlag(cmd))
    }

    /**
     * Calls pip command and return output as string. Preferred way instead of direct python usage to correctly
     * apply --user flag for commands.
     *
     * @param cmd pip command to call
     * @return command execution output
     */
    String readOutput(String cmd) {
        python.withHiddenLog {
            python.readOutput("-m pip ${applyUserFlag(cmd)}")
        }
    }

    /**
     * May be used to change default configurations.
     *
     * @return python cli instance used to execute commands
     */
    Python getPython() {
        return python
    }

    /**
     * @return pip version string (minor.major.micro)
     */
    @Memoized
    String getVersion() {
        python.withHiddenLog {
            python.readOutput('-c "import pip; print(pip.__version__)"')
        }
    }

    /**
     * @return pip --version output
     */
    @Memoized
    String getVersionLine() {
        readOutput('--version')
    }

    private String applyUserFlag(String cmd) {
        if (!cmd.contains(USER) && userScope
                && USER_AWARE_COMMANDS.contains(cmd.split(' ')[0].toLowerCase())) {
            cmd += " $USER"
        }
        cmd
    }
}
