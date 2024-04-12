# Configuration

## Python location

On linux, plugin will use `python3` if available (and fall back to `python` if not). To use different binary use:

```groovy
python {
    pythonBinary = 'python'
}
```

This will force python 2 for linux. Also, this may be handy if python binary is named differently.

To use non-global python:

```groovy
python {
    pythonPath = 'path/to/python/binary/'
}
```

`pythonPath` must be set to directory containing python binary (e.g. 'path/to/python/binary/python.exe')

!!! note
    `pythonPath` is ignored when virtualenv used (virtualenv located at `python.envPath` already exists).

!!! important
    If python can't be found, please pay attention to PATH: most likely, it is different from your shell PATH
    (for example, this might happen when using pyenv). To check PATH, just put somewhere inside build script:

    ```groovy
    println System.getenv('PATH')
    ```

## Minimal python and pip versions

To set python version constraint:

```groovy
python {
    minPythonVersion = '3.2'
}
```

Python version format is: major.minor.micro.
Constraint may include any number of levels: '3', '3.1', '2.7.5'

The same way pip version could be restricted:

```groovy
python {
    minPipVersion = '9.0.1'
}
```

## Virtualenv

When you declare any pip modules, plugin will try to use [venv](https://docs.python.org/3/library/venv.html) or 
[virtualenv](https://virtualenv.pypa.io/en/stable/) to install required modules locally (for current project only).

By default, plugin would use [venv](https://docs.python.org/3/library/venv.html), usually bundled with python (since 3.3). 
Only if venv is not detected, plugin would fall back to [virtualenv](https://virtualenv.pypa.io/en/stable/).

If, for some reason, you don't want to use venv at all and prefer virtualenv:

```groovy
python.useVenv = false
```

!!! note
    All virtualenv-related options are ignored when venv is detected (and not disabled to use).

If virtualenv is not installed - it will be installed automatically in `--user` scope. If you don't want automatic
installation then disable it:

```groovy
python.installVirtualenv = false
```

Plugin installs exact pip version declared in `python.virtualenvVersion` (by default, 20.25.1).
This way, plugin will always install only known to be working version and avoid side effects of "just released"
versions (note that pip 20 is a major rewrite and may still contain side effects).

In any case, plugin checks if virtualenv is already installed and use it to create local environment
(if not, then fall back to  `--user` scope by default). Virtualenv usage is driven by declared scope, so if you don't want to use it set:

```groovy
python.scope = USER // or GLOBAL
```

With USER (or GLOBAL) scope, virtualenv will not be used, even if it's already created in project (plugin will ignore it and use global python).

If you already use virtualenv in your project (have created manually environment), then simply point plugin to use it:

```groovy
python.envPath = 'path/to/your/env'
```

!!! tip
    envPath value might contain user home reference like `envPath = '~/.myproject`.
    This might be useful for CI where created environment should be cached. 

It will automatically change `pythonPath` configuration accordingly.

!!! note 
    Plugin will *not* create environment if you don't use any modules. If you still want to use project specific environment
    (without declared pip modules) then create it manually: `python3 -m virtualenv .gradle/python` (default location). Plugin will recognize
    existing env and use it.

!!! important 
    Virtualenv creates local python copy (by default in `.gradle/python`). Copy is created from global python and
    later used *instead* of global python. If you want to change used python version in the environment,
    then manually remove `.gradle/python` so it could be created again (from global python).

To copy environment instead of symlinking (default) set ([--always-copy](https://virtualenv.pypa.io/en/stable/reference/#cmdoption-always-copy) (virtualenv) 
or [--copies](https://docs.python.org/3/library/venv.html) (venv)):

```groovy
python.envCopy = true
```

## Pip

By default, all installed python modules are printed to console after pip installations
using `pip list` (of course, if at least one module declared for installation).
This should simplify problems resolution (show used transitive dependencies versions).

To switch off:

```groovy
python {
    showInstalledVersions = false
}
```

You can always see the list of installed modules with `pipList` task (exactly the same list as after pipInstall).

!!! note 
    If global python is used with USER scope and some modules were manually installed in global scope
    then they will not be shown by pipList (and after pip install). To see all modules:

    ```groovy
    pipList.all = true
    ```

Global modules are hidden by default (for USER scope) because on linux there are a lot of system modules pre-installed.

By default, 'pip install' is not called for modules already installed with correct version.
In most situations this is preferred behaviour, but if you need to be sure about dependencies
then force installation:

```groovy
python {
    alwaysInstallModules = true
}
```

## Reference

All configuration options with default values:

```groovy
python {
   // path to python binary (global by default)
   pythonPath
   // python binary name (python or python3 by default)
   pythonBinary
   // search python in system PATH and fail build if not found (incorrect PATH reveal)
   validateSystemBinary = true 
   // additional environment variables, visible for all python commands
   environment = [:]
   
   // minimal required python version (m.m.m)
   minPythonVersion
   // minimal required pip version (m.m.m)
   minPipVersion = '9'   
   
   // show all installed modules versions after pip installation
   showInstalledVersions = true
   // always call module install, even if correct version is already installed
   alwaysInstallModules = false
   // may be used to disable pip cache (--no-cache-dir option)
   usePipCache = true
   // required to overcome "error: externally-managed-environment" error on linux (--break-system-packages)
   breakSystemPackages = false
   // additional pip repositories (--extra-index-url option)
   extraIndexUrls = []
   // trusted hosts for pip install (--trusted-host option)
   trustedHosts = []

   
   // pip modules installation scope (project local, os user dir, global) 
   scope = VIRTUALENV_OR_USER
   // use venv instead of virtualenv (auto fall back to virtualenv if not found)
   useVenv = true 
   // automatically install virtualenv module (if pip modules declared and scope allows)   
   installVirtualenv = true
   // if virtualenv not installed (in --user scope), plugin will install exactly this version
   // (known to be working version) to avoid side effects
   virtualenvVersion = '20.25.1'
   // minimal required virtualenv (v20 is recommended, but by default 16 set to not fail previous
  // setups)
   minVirtualenvVersion = '16'
   // used virtualenv path (if virtualenv used, see 'scope')
   envPath = '.gradle/python'
   // copy virtualenv instead of symlink when created (venv --copies and virtualenv --always-copy)
   envCopy = false
   // print stats for all executed python command (including hidden)  
   printStats = false 

   requirements {
       // use requirements.txt file
       use = true
       // file to use as requirements (path to file)
       file = 'requirements.txt'
       // requirements restriction (exact versions, syntax subset support)
       // false delegates requirements loading to pip
       strict = true
   }
    
   docker {
      // enables docker support 
      use = false
      // image to use 
      image = '{{ gradle.image }}'
      // windows containers indicator (not supported now, done for the future) 
      windows = false
      // use host network directly (works only on linux) 
      useHostNetwork = false 
      // docker ports to expose into host (direct 5000 or mapped '5000:6000')
      ports = [] 
   } 
}
```

Note that in case of multi-module project envPath is set to '.gradle/python' inside the root project,
even if plugin is activated inside module (see [multi-module setup](multimodule.md)).

### PythonTask

PythonTask configuration:

| Property              | Description                                                                                                                            |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| pythonPath            | Path to python binary. By default, property ignored because checkPython task selects correct path                                      |
| useCustomPath         | Force pythonPath property use instead of path selected by checkPython task (e.g. to use global python instead of environment)          |
| pythonBinary          | Python binary name. By default, python3 on linux and python otherwise.                                                                 |
| validateSystemBinary  | Search python binary in PATH and fail build to reveal PATH problems                                                                    |
| workDir               | Working directory (important if called script/module do file operations). By default, it's a project root                              |
| createWorkDir         | Automatically create working directory if does not exist. Enabled by default                                                           |
| module                | Module name to call command on (if command not set module called directly). Useful for derived tasks.                                  |
| command               | Python command to execute (string, array, iterable)                                                                                    |
| logLevel              | Logging level for python output. By default is `LIFECYCLE` (visible in console). To hide output use `LogLevel.INFO`                    |
| pythonArgs            | Extra python arguments applied just after python binary. Useful for declaring common python options (-I, -S, etc.)                     |
| extraArgs             | Extra arguments applied at the end of declared command (usually module arguments). Useful for derived tasks to declare default options |
| outputPrefix          | Prefix, applied for each line of python output. By default is '\t' to identify output for called gradle command                        |
| environment           | Process specific environment variables                                                                                                 |
| docker.use            | Enable docker support                                                                                                                  |
| docker.image          | Python image to use                                                                                                                    |
| docker.windows        | Windows image use. Not usefule now as testcontainers can't run on windows containers (imlpemented for the future)                      |
| docker.useHostNetwork | Use host network in container (exposed ports ignored in this case). Only for linux (ignored on other platforms)                        |
| docker.ports          | Exposed ports from docker container                                                                                                    |
| docker.exclusive      | Enable exclusive container mode (immediate logs for long-running tasks)                                                                |


Also, task provide extra methods:

* `pythonArgs(String... args)` to declare extra python arguments (shortcut to append values to pythonArgs property).
* `extraArgs(String... args)` to declare extra arguments (shortcut to append values to extraArgs property).
* `environment(String var, Object value)` to set custom environment variable (shortcut to append values to environment property)
* `environment(Map<String, Object> vars)` to set multiple custom environment variables at once (shortcut to append values to environment property)
* `docker.ports(Object... ports)` to set container ports to expose (direct 5000 or mapped '5000:6000')
* `dockerChown(<String or Path> path)` to [fix root user](docker.md#user-permissions) on paths created inside container for linux
* `dockerExec(Object command)` to [run native command](docker.md#docker-commands) inside container

### PipInstallTask

Default pip installation task is registered as `pipInstall` and used to install modules, declared in global configuration.
Custom task(s) may be used, if required:

```groovy
tasks.register('myPipInst', PipInstallTask) {
    pip 'mod:1', 'other:2'
}
```

Configuration:

| Property                                      | Description                                                                                                                                         |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| pythonPath                                    | Path to python binary. By default, property ignored because checkPython task selects correct path                                                   |
| useCustomPath                                 | Force pythonPath property use instead of path selected by checkPython task (e.g. to use global python instead of environment)                       |
| pythonBinary                                  | Python binary name. By default, python3 on linux and python otherwise.                                                                              |
| validateSystemBinary                          | Search python binary in PATH and fail build to reveal PATH problems                                                                                 |
| pythonArgs                                    | Extra python arguments applied just after python binary. Useful for declaring common python options (-I, -S, etc.)                                  |
| environment                                   | Process specific environment variables                                                                                                              |
| modules                                       | Modules to install. In most cases configured indirectly with `pip(..)` task methods. By default, modules from global configuration.                 |
| userScope                                     | Use current user scope (`--user` flag). Enabled by default to avoid permission problems on *nix (global configuration).                             |
| showInstalledVersions                         | Perform `pip list` after installation. By default use global configuration value                                                                    |
| alwaysInstallModules                          | Call `pip install module` for all declared modules, even if it is already installed with correct version. By default use global configuration value |
| useCache                                      | Can be used to disable pip cache (--no-cache-dir)                                                                                                   |
| extraIndexUrls                                | Additional pip repositories (--extra-index-url)                                                                                                     |
| trustedHosts / trusted hosts (--trusted-host) |
| options                                       | additional pip install options                                                                                                                      |
| requirements                                  | Requirements file to use                                                                                                                            |
| strictRequirements                            | Strict or native requirements file processing mode                                                                                                  |
| envPath                                       | Virtual environment path (require to chown dir inside docker)                                                                                       | 

And, as shown above, custom methods:

* `pip(String... modules)`
* `pip(Iterable<String> modules)`
* `options(String... options)`
