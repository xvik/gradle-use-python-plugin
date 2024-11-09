package ru.vyarus.gradle.plugin.python

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.task.pip.module.ModuleFactory

/**
 * Use-python plugin extension. Used to declare python location (use global if not declared) and specify
 * required modules to install.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
@CompileStatic
@SuppressWarnings(['ConfusingMethodName', 'ClassSize'])
class PythonExtension {

    /**
     * Path to python binary (folder where python executable is located). When not set, global python is called.
     * NOTE: automatically set when virtualenv is used (see {{@link #scope}).
     */
    String pythonPath
    /**
     * Python binary name to use. By default, for linux it would be python3 (if installed) or python and
     * for windows - python.
     * <p>
     * Useful when you want to force python 2 usage on linux or python binary name differs.
     */
    String pythonBinary

    /**
     * Manual search for global python binary (declared in {@link #pythonBinary} in system paths (PATH variable)).
     * This should quickly reveal cases when process PATH is different from shell path (which could happen, for example,
     * with pyenv, when PATH modified only in bashrc and gradle is called not from shell).
     * <p>
     * Validation is performed only when {@link #pythonPath} is not declared (because otherwise it does not make sense).
     * <p>
     * Option was added ONLY to be able to disable validation in edge cases when validation behave incorrectly.
     */
    boolean validateSystemBinary = true

    /**
     * System environment variables for executed python process (variables specified in gradle's
     * {@link org.gradle.process.ExecSpec#environment(java.util.Map)} during python process execution).
     */
    Map<String, Object> environment = [:]

    /**
     * Minimal required python version. Full format: "major.minor.micro". Any precision could be used, for example:
     * '3' (python 3 or above), '3.2' (python. 3.2 or above), '3.6.1' (python 3.6.1 or above).
     */
    String minPythonVersion

    /**
     * Minimal required pip version. Format is the same as python version: "major.minor.micro". Any precision could
     * be used (9, 8.2, etc). By default pip 9 is required.
     */
    String minPipVersion = '9'

    /**
     * Pip modules to install. Modules are installed in the declaration order.
     * Duplicates are allowed: only the latest declaration will be used. So it is possible
     * to pre-define some modules (for example, in plugin) and then override module version
     * by adding new declaration (with {@code pip (..)} method).
     * <p>
     * Use {@code pip (..)} methods for modules declaration.
     */
    List<String> modules = []

    /**
     * Calls 'pip list' to show all installed python modules (for problem investigations).
     * Enabled by default for faster problems detection (mostly to highlight used transitive dependencies versions).
     */
    boolean showInstalledVersions = true

    /**
     * By default, plugin will not call "pip install" for modules already installed (with exactly
     * the same version). Enable option if you need to always call "pip install module" (for example,
     * to make sure correct dependencies installed).
     * <p>
     * For requirements file this option might be useful if requirements file links other files, which changes plugin
     * would not be able to track.
     */
    boolean alwaysInstallModules

    /**
     * By default, pip cache all installed packages. Sometimes this may lead to incorrect dependency version
     * resolution (when newer version from cache installed instead of specified). Since pip 20 vcs dependencies
     * (module build from vcs revision) are also cached, so if you need to force rebuild each time you will
     * need to disable cache (may be useful for testing).
     * <p>
     * Essentially, disabling this option adds {@code --no-cache-dir} for all pip calls.
     *
     * @see <a href="https://pip.pypa.io/en/stable/topics/caching/#disabling-caching">--no-cache-dir</a>
     */
    boolean usePipCache = true

    /**
     * Option required to overcome "error: externally-managed-environment" error. Such error could appear on linux
     * when non-default python used, installed by apt: e.g. python3.12 installed as "apt install python3.12".
     * In this case python prevents user to use pip for system modules installation and assume apt usage instead.
     * This option applies "--break-system-packages" flag in install task to hide this error. Modules would be
     * installed into local user directory (like ~/.local/lib/python3.12).
     *
     * @see <a href="https://pip.pypa.io/en/stable/cli/pip_install/#cmdoption-break-system-packages">
     *     --break-system-packages</a>
     */
    boolean breakSystemPackages = false

    /**
     * This can be used if you want to use your own pypi instead of default global one.
     * Applies to {@code pip install}, {@code pip download}, {@code pip list} and {@code pip wheel}.
     *
     * @see <a href="https://pip.pypa.io/en/stable/cli/pip_install/#cmdoption-i">--index-url</a>
     */
    String indexUrl

    /**
     * This can be used to if you host your own pypi besides the default global one.
     * Applies to {@code pip install}, {@code pip download}, {@code pip list} and {@code pip wheel}.
     *
     * @see <a href="https://pip.pypa.io/en/stable/cli/pip_install/#cmdoption-extra-index-url">--extra-index-url</a>
     */
    List<String> extraIndexUrls = []

    /**
     * Mark a host as trusted for pip even when it has no tls, or the certificate is invalid.
     * Can be used in combination with {@link #extraIndexUrls} to use your own pypi server.
     * Applies only for {@code pip install} (other commends does not support this option).
     *
     * @see <a href="https://pip.pypa.io/en/stable/cli/pip/#cmdoption-trusted-host">--trusted-host</a>
     */
    List<String> trustedHosts = []

    /**
     * Target scope for pip packages installation.
     * NOTE: Enum values might be specified directly like {@code USER} or {@code GLOBAL} (shortcuts registered by
     * plugin).
     */
    Scope scope = Scope.VIRTUALENV_OR_USER
    /**
     * Use venv instead of virtualenv (because it does not need to be installed for python 3). Note that already
     * existing environment, created with virtualenv, would still be completely working (the same is true for venv
     * environment), so it is safe to put any value.
     * <p>
     * Note that on some linux distributions, venv must be installed with python3-venv package. Plugin would
     * not attempt to install it automatically - instead it would fall back to virtualenv usage.
     * <p>
     * Also note that in case of venv, pip version would be different from globally installed pip (because venv
     * install custom pip inside environment, whereas virtualenv links global).
     */
    boolean useVenv = true
    /**
     * Automatically install virtualenv (if pip modules used). Will install for current user only (--user).
     * Installed virtualenv will not be modified later, so you will need to manually update it in case of problems.
     * When disabled, plugin will still check if virtualenv is available, but will not try to install it if not.
     * <p>
     * Ignored for venv ({@link #useVenv})
     */
    boolean installVirtualenv = true
    /**
     * Used only when virtualenv is installed automatically ({@link #installVirtualenv}). It's better to install
     * exact version, known to work well to avoid side effects. For example, virtualenv 20.0.x is a major rewrite and
     * it contains some minor regressions in behaviour (which would be fixed eventually, ofc).
     * <p>
     * You can set null or empty value to install the latest version.
     * <p>
     * Property not used at all if virtualenv is already installed (no warning on version mismatch): if you installed
     * it manually then you know what you are doing, otherwise "known to be working" version should be installed to
     * avoid problems.
     * <p>
     * Ignored for venv ({@link #useVenv})
     */
    String virtualenvVersion = '20.25.1'
    /**
     * Minimal required virtualenv version. It is recommended to use virtualenv 20 instead of 16 or older because
     * it was a complete rewrite (and now it is stable enough). The most important moment is pip installation
     * behaviour: virtualenv &lt;=16  was always downloading the latest pip which compromise build stability.
     * <p>
     * Default value set to 16 to not fail environments, created with the previous plugin versions. But it
     * is recommended to use at least 20.0.11 (because this version fixes Windows Store compatibility
     * (https://github.com/pypa/virtualenv/issues/1709)). Older virtualenvs could also be used!
     * <p>
     * Specified version could actually have any precision: '20', '20.1', etc.
     * <p>
     * Ignored for venv ({@link #useVenv})
     */
    String minVirtualenvVersion = '16'
    /**
     * Virtual environment path to use. Used only when {@link #scope} is configured to use virtualenv.
     * Virtualenv will be created automatically if not yet exists.
     * <p>
     * By default env path is set to '.gradle/python' inside the root project (!). This avoids virtualenv duplication
     * in the multi-module setup (all modules will use the same env and so pip modules may be installed only once).
     * <p>
     * If multiple env required (e.g. to use different python versions or to separate pip initialization (e.g.
     * for incompatible modules)) declare env path manually in each module.
     * <p>
     * User home directory reference will be automatically resolved in path ("~/somewhere/"). This might be useful
     * for CI environments where environment directory should be cached, but it can't be located inside project.
     */
    String envPath
    /**
     * Copy virtual environment instead of symlink (see --always-copy virtualenv and --copies venv option).
     * By default use default virtualenv behaviour: symlink environment.
     */
    boolean envCopy

    /**
     * Print all executed python commands at the end of the build (including "invisible" internal commands, used by
     * plugin for configuration).
     * <p>
     * IMPORTANT: in multi-module project only root project setting takes effect
     */
    boolean printStats = false

    /**
     * Enable debug messages (for cache and stats debugging). See {@link #printStats} for printing actually called
     * commands (including invisible). Option for development purposes (especially with configuration cache).
     * <p>
     * IMPORTANT: in multi-module project only root project setting takes effect
     */
    boolean debug = false

    /**
     * Requirements file support. By default, plugin will search for requirements.txt file and would read all
     * declarations from there and use for manual installation (together with `python.modules` property). File could
     * contain only the same declarations as direct modules declarations in gradle file (only exact version numbers).
     * This way is more secure and allows other tools to read or update this file.
     * <p>
     * Pure python behaviour is also supported in non-strict mode (see {@link Requirements#strict}).
     */
    Requirements requirements = new Requirements()

    /**
     * Execute python inside docker container. When enabled, project (root) mapped to container and all
     * python executions appear inside docker.
     */
    Docker docker = new Docker()

    private final Project project

    PythonExtension(Project project) {
        this.project = project
        // by default storing environment inside the root project and use relative path (for simpler logs)
        envPath = project.relativePath(project.rootProject.file('.gradle/python'))
    }

    /**
     * Shortcut for {@link #pip(java.lang.Iterable)}.
     *
     * @param modules pip modules to install
     */
    void pip(String... modules) {
        pip(Arrays.asList(modules))
    }

    /**
     * Declare pip modules to install. Format: "moduleName:version". Only exact version
     * matching is supported (pip support ranges, but this is intentionally not supported in order to avoid
     * side effects).
     * <p>
     * Feature syntax is also allowed: "moduleName[qualifier]:version". As it is impossible to detect enabled features
     * of installed modules then qualified module will not be installed if module with the same name and
     * version is already installed (moduleName:version).
     * <p>
     * VCS module declaration is also supported in format: "vcs+protocol://repo_url/@vcsVersion#egg=pkg-pkgVersion".
     * Note that it requires both vcs version (e.g. commit hash) and package version in order to be able to perform
     * up-to-date check. You can specify branch name or tag (for example, for git) instead of direct hash, but
     * prefer using exact version (hash or tags, not branches) in order to get reproducible builds.
     * See <a href="https://pip.pypa.io/en/stable/topics/vcs-support/">pip vcs docs</p>.
     * <p>
     * Duplicate declarations are allowed: in this case the latest declaration will be used.
     * <p>
     * Note: if newer package version is already installed, pip will remove it and install correct version.
     *
     * @param modules pip modules to install
     */
    void pip(Iterable<String> modules) {
        this.modules.addAll(modules)
    }

    /**
     * Change default pypi repository. Applies only for some pip tasks (see {@link #indexUrl}).
     *
     * @param url default pip repository
     */
    void indexUrl(String url) {
        this.indexUrl = url
    }

    /**
     * Shortcut for {@link #extraIndexUrls(java.lang.String [ ])}.
     *
     * @param urls extra pip repositories
     */
    void extraIndexUrls(String... urls) {
        extraIndexUrls(Arrays.asList(urls))
    }

    /**
     * Add additional pip repositories to use. Applies only for some pip tasks (see {@link #extraIndexUrls}).
     *
     * @param urls extra pip repositories
     */
    void extraIndexUrls(Iterable<String> urls) {
        this.extraIndexUrls.addAll(urls)
    }

    /**
     * Shortcut for {@link #trustedHosts(java.lang.Iterable)}.
     *
     * @param hosts trusted hosts (host only or with port).
     */
    void trustedHosts(String... hosts) {
        trustedHosts(Arrays.asList(hosts))
    }

    /**
     * Add trusted hosts for pip. Applies only for {@code pip install} (see {@link #trustedHosts}).
     *
     * @param hosts hosts trusted hosts (host only or with port).
     */
    void trustedHosts(Iterable<String> hosts) {
        this.trustedHosts.addAll(hosts)
    }

    /**
     * Shortcut to set environment variables for all python tasks.
     *
     * @param name variable name
     * @param value variable value
     */
    void environment(String name, Object value) {
        environment.put(name, value)
    }

    /**
     * Pip dependencies installation scope.
     */
    static enum Scope {
        /**
         * Global scope (all users). May require additional rights on linux.
         */
        GLOBAL,
        /**
         * Current user (os) only. Packages are installed into the user specific (~/) directory.
         */
        USER,
        /**
         * Same behaviour as {@link #VIRTUALENV} when virtualenv module installed (possibly automatically), but if
         * module not found, falls back to user scope (same as {@link #USER}, instead of creating local environment).
         */
        VIRTUALENV_OR_USER,
        /**
         * Use virtualenv. virtualenv must be either installed manually or requested for automatic installation
         * {@link PythonExtension#installVirtualenv}). Will fail if mode not installed (either way).
         * If virtualenv is not created, then it would be created automatically.
         * Path of environment location is configurable (by default cached in .gradle dir).
         */
        VIRTUALENV
    }

    /**
     * Checks if module is already declared (as usual or vcs module). Could be used by other plugins to test
     * if required module is manually declared or not (in order to apply defaults).
     * <p>
     * NOTE: this check ignores modules in requirements file!
     *
     * @param name module name to check
     * @return true of module already declared, false otherwise
     */
    boolean isModuleDeclared(String name) {
        return ModuleFactory.findModuleDeclaration(name, modules) != null
    }

    /**
     * Utility method to support requirements configuration.
     *
     * @param config configuration action
     */
    void requirements(Action<Requirements> config) {
        config.execute(this.requirements)
    }

    /**
     * Configure docker container.
     *
     * @param config configuration action
     */
    void docker(Action<Docker> config) {
        config.execute(this.docker)
    }

    /**
     * Requirements file support.
     */
    static class Requirements {

        /**
         * Use requirements file. False to ignore it, even if exists.
         */
        boolean use = true

        /**
         * Requirements file path, relative to project root (in case of sub module - sub module root) or absolute path.
         */
        String file = 'requirements.txt'

        /**
         * Strict mode: only exact version declarations allowed (the same declarations as in gradle file). In strict
         * mode plugin reads requirements file and use them the same way as if they would be declared directly in
         * gradle file. This is more secure and allows external tools to read and update requirements.
         * <p>
         * Strict mode supports file reference: -r some-other.txt. Sub files would be also parsed.
         * <p>
         * By disabling strict mode, requirements file processing delegated to pip. Which means that there would
         * be no restrictions on pip file format.
         *
         * @see <a href="https://pip.pypa.io/en/stable/reference/requirements-file-format/#requirements-file-format">
         *     format</a>
         */
        boolean strict = true
    }

    /**
     * Docker support is implemented with <a href="https://testcontainers.org">testcontainers</a>.
     * Container always removed after gradle execution (and so container state wiped out between builds). Root
     * project directory is mounted into docker and all paths in executed command are automatically rewritten, so
     * enabling docker should be almost invisible.
     * <p>
     * If you use pip modules then prefer virtualenv creation (default) - this way python environment would be
     * created from docker inside project "caching" pip install task result.
     * <p>
     * Be aware, that using docker would slow down your builds.
     * To speed-up execution, docker container is started on first request and stay alive until gradle execution
     * ends. When different containers used (e.g. in different modules), they all would be started once and used
     * through entire gradle execution.
     * <p>
     * Each python task has it's own docker configuration. By default, it would be the same as in extension, but
     * could be changed manually. If task configured differ (work dir, environment or specific docker config)
     * then docker container would be re-started, applying exact task configuration.
     * <p>
     * LIMITATION: output from docker command would appear only after command execution. For long-lived
     * process where output is important use
     * {@link ru.vyarus.gradle.plugin.python.task.BasePythonTask.DockerEnv#getExclusive()} flag directly on task.
     * This way, task would start new container, using python command as container command.
     * <p>
     * Be aware that currently testcontainers does not work on windows server and so it would be impossible to run
     * it on CI like appveyor (<a href="https://www.testcontainers.org/supported_docker_environment/windows/">
     * see official docs</a>)!
     */
    static class Docker {
        /**
         * Enable docker support. When disabled, direct python is used.
         */
        boolean use

        /**
         * Type of used image. By default, linux images used. Required for proper work on windows images.
         * <p>
         * WARNING: plugin supports windows images in theory, but this wasn't tested because testcontainers does not
         * support currently windows containers (WCOW). So right now this option is completely useless.
         *
         * @see <a href="https://www.testcontainers.org/supported_docker_environment/windows/">windows support</a>
         */
        boolean windows

        /**
         * Docker image to use. This is complete image path (potentially including repository and tag) and not just
         * image name. It is highly suggested always specifying exact tag!
         * <p>
         * Plugin use linux image by default. On windows linux containers must be used (WSL2 or Hyper-V) because
         * testcontainers currently does not support windows containers (plugin implements windows support for the
         * future).
         *
         * @see <a href="https://hub.docker.com/_/python">python image</a>
         */
        String image = 'python:3.12.7-alpine3.20'

        /**
         * Use host network instead of custom isolated network. This might be useful to speed up execution in some
         * cases (due to absence of NAT) or to overcome some network problems (e.g. enabled VPN on host may cause
         * problems for container external connections),
         * <p>
         * Works only on linux! On other systems would be simply ignored.
         * <p>
         * When enabled, exposed ports could not be used! All container ports would be already available
         * (because of shared network). Also, all host ports are available to container (again, due to same network).
         *
         * @see <a href="https://docs.docker.com/network/drivers/host/">docs</a>
         */
        boolean useHostNetwork = false

        /**
         * Required container port mappings - port to open from container to be accessible on host.
         * Note that normally ports are not required because python code executed inside container. This could
         * make sense for long-lived process like dev.server.
         * <p>
         * Single number (2011) for mapping on the same port and colon-separated numbers (2011:3011) for mapping
         * on custom port.
         */
        Set<String> ports = []

        /**
         * Specify ports to expose from container. Value could be either integer or string. By default, port would
         * be mapped on the same port on host (no random), but if different port is required use 'port:port' string.
         *
         * @param ports ports to be mapped from container
         */
        void ports(Object... ports) {
            ports.each { this.ports.add(String.valueOf(it)) }
        }
    }
}
