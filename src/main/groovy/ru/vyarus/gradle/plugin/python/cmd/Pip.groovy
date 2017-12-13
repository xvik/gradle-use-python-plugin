package ru.vyarus.gradle.plugin.python.cmd

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

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
        exec("uninstall $module -y -q")
    }

    /**
     * Execute command on pip module. E.g. 'install some==12.3'.
     *
     * @param cmd pip command to execute
     */
    void exec(String cmd) {
        // automatically apply user scope
        if (!cmd.contains(USER) && userScope &&
                USER_AWARE_COMMANDS.contains(cmd.split(' ')[0].toLowerCase())) {
            cmd += " $USER"
        }
        python.callModule('pip', cmd)
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
        python.withHiddenLog {
            python.readOutput('-m pip --version')
        }
    }
}
