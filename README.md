# Gradle use-python plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-use-python-plugin.svg)](https://travis-ci.org/xvik/gradle-use-python-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-use-python-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-use-python-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-use-python-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-use-python-plugin)

### About

Plugin **does not install python and pip** itself and use globally installed python (by default). 
It's easier to prepare python manually because python have good compatibility (from user perspective) and does not need to 
be updated often.

The only plugin intention is to simplify python usage from gradle. By default, plugin creates python virtualenv
inside the project and installs all modules there so each project has its own python (copy) and could not be 
affected by other projects or system changes.

Features:

* Install required python modules using pip (per project (virtualenv), os user (--user) or globally) 
* Provides task to call python commands, modules or scripts (`PythonTask`)
* Could be used as basement for building plugins for specific python modules (like 
[mkdocs plugin](https://github.com/xvik/gradle-mkdocs-plugin))

##### Summary

* Configuration: `python`
* Tasks:
    - `checkPython` - validate python installation (and create virtualenv if required)
    - `pipInstall` - install declared pip modules
    - `pipUpdates` - show the latest available versions for the registered modules
    - `pipList` - show all installed modules (the same as pipInstall shows after installation)
    - `type:PythonTask` - call python command/script/module
    - `type:PipInstallTask` - may be used for custom pip modules installation workflow

##### Possible pip issue warning (linux/macos)

If `pip3 list -o` fails with: `TypeError: '>' not supported between instances of 'Version' and 'Version'`
Then simply update installed pip version: `python3 -m pip install --upgrade pip`

This is a [known issue](https://github.com/pypa/pip/issues/3057) related to incorrectly 
patched pip packages in some distributions.  

### Setup

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-use-python-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-use-python-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-use-python-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-use-python-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/use-python/ru.vyarus.use-python.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.use-python)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-use-python-plugin:1.2.0'
    }
}
apply plugin: 'ru.vyarus.use-python'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.use-python' version '1.2.0'
}
```  

#### Compatibility

Plugin compiled for java 7.
Compatible with gradle 4 and above.

#### Python & Pip

Make sure python and pip are installed:

```bash
python --version  
pip --version
```

On *nix `python` usually reference python2. For python3:

```bash
python3 --version  
pip3 --version
```

##### Windows install

[Download and install](https://www.python.org/downloads/windows/) python manually or use 
[chocolately](https://chocolatey.org/packages/python/3.6.3): 

```bash
choco install python
```

##### Linux/Macos install

On most *nix distributions python is already installed, but often without pip.
 
[Install](https://pip.pypa.io/en/stable/installing/) pip if required (ubuntu example):

```bash
sudo apt-get install python3-pip
```

Make sure the latest pip installed (required to overcome some older pip problems):

```bash
pip3 install -U pip
```

Note that on ubuntu pip installed with `python3-pip` package is 9.0.1, but it did not(!) downgrade
module versions (e.g. `pip install click 6.6` when click 6.7 is installed will do nothing). 
Maybe there are other differences, so it's highly recommended to upgrade pip with `pip3 install -U pip`.

#### Automatic pip upgrade

As described above, there are different ways of pip installation in linux and, more important,
admin permissions are required to upgrade global pip. So it is impossible to upgrade pip from plugin (in all cases).

But, it is possible inside virtualenv or user (--user) scope. Note that plugin creates virtualenv by default (per project independent python environment).

So, in order to use newer pip simply put it as first dependency:

```
python {
    pip 'pip:10.0.1'
    pip 'some_module:1.0'
}
```

Here project virtualenv will be created with global pip and newer pip version installed inside environment. 
Packages installation is sequential, so all other packages will be installed with newer pip (each installation is independent pip command).

The same will work for user scope: `python.scope = USER`

When applying this trick, consider minimal pip version declared in configuration
(`python.minPipVersion='9'` by default) as minimal pip version required for *project setup* 
(instead of minimal version required *for work*).

#### Automatic python install

Python is assumed to be used as java: install and forget. It perfectly fits user
use case: install python once and plugin will replace all manual work on project environment setup. 

It is also easy to configure python on CI (like travis).    

If you want automatic python installation, try looking on JetBrain's 
[python-envs plugin](https://github.com/JetBrains/gradle-python-envs). But be careful because 
it has some caveats (for example, on windows python could be installed automatically just once 
and requires manual un-installation). 

#### Multi-module projects

When used in multi-module project, plugin will create virtualenv inside the root project directory
in order to share the same environment for all modules.

See [multi-module setup cases](https://github.com/xvik/gradle-use-python-plugin/wiki/Multi-module-projects) 

#### Travis CI configuration

To make plugin work on [travis](https://travis-ci.org/) you'll need to install python3 packages:

```yaml
language: java  
dist: xenial
jdk: openjdk8

sudo: required
addons:
  apt:
    packages:
    - "python3"
    - "python3-pip"
    - "python3-setuptools" 

before_install:
  - sudo pip3 install -U pip
``` 

It will be python 3.5 by default.

NOTE: travis does not require manual `sudo` support enable anymore (enabled by default) 

#### Appveyour CI configuration

To make plugin work on [appveyour](https://www.appveyor.com/) you'll need to add python to path:

```yaml
environment:
    matrix:
        - JAVA_HOME: C:\Program Files\Java\jdk1.8.0
          PYTHON: "C:\\Python35-x64"

install:
  - set PATH=%PYTHON%;%PYTHON%\\Scripts;%PATH%
```         

Now plugin would be able to find python binary.   

### Usage

Declare required modules (optional):

```groovy
python.pip 'module1:1.0', 'module2:1.0'
```

or 

```groovy
python {
    pip 'module1:1.0'
    pip 'module2:1.0'
}
```

Module format is: `name:version` (will mean `name==version` in pip notion). Non strict version definition is not allowed (for obvious reasons).
Dependencies are installed in declaration order. If duplicate declaration specified then only the latest declaration
will be used:

```groovy
python.pip 'module1:2.0', 'module2:1.0', 'module1:1.0' 
```

Will install version 1.0 of module1 because it was the latest declaration. Module overrides
work for all declaration types (see below): the last declared module is used.

Dependencies are installed with `pipInstall` task which is called before any declared `PythonTask`.

Note that by default dependencies are installed inside project specific virtualenv (project specific copy of python environment).

#### Pip module extra features

You can declare modules with [extra features](https://setuptools.readthedocs.io/en/latest/setuptools.html#declaring-extras-optional-features-with-their-own-dependencies) 
in module name to install special version of module (with enabled features):

```groovy
python.pip 'requests[socks,security]:2.18.4'
```

IMPORTANT: it is impossible to track if this "variation" of module is installed, so
plugin performs up-to-date check for such modules by name only (for example, 
if 'requests==2.18.4' is already installed). For most cases, this is suitable behaviour because, by default, 
modules are installed in virtualenv and so you will always have correct module installed.
For other cases, you can disable up-to-date checks (delegate all dependencies logic to pip): `python.alwaysInstallModules = true` 

#### VCS pip modules

You can declare [vcs modules](https://pip.pypa.io/en/stable/reference/pip_install/#vcs-support): modules installed directly from 
version control (e.g. git, svn). Format: 

```
vcs+protocol://repo_url/@vcsVersion#egg=pkg-pkgVersion
```

* `@vcsVersion` part is required: prefer using commit version or tag for reproducible builds
* `-pkgVersion` is installed module version. Required to be able to compare declared plugin with installed version.

For example:

```groovy
python.pip 'git+https://github.com/ictxiangxin/boson/@b52727f7170acbedc5a1b4e1df03972bd9bb85e3#egg=boson-0.9'
```

Declares module `boson` version `0.9`, installed from git commit `b52727f7170acbedc5a1b4e1df03972bd9bb85e3` (it may be tag name or branch, but prefer not using branch names).

`pipInstall` will be considered up-to-date if `boson==0.9` is already installed. Note that declared module version
is completely free: you can set any version (0.10, 1.2, etc.), it is not checked and used only for up-to-date validation. 

Vcs module installation is: source checkout and module build (using setup.py). You may need to specify subdirectory
as `&subdirectory=pkg_dir` ([see docs](https://pip.pypa.io/en/stable/reference/pip_install/#vcs-support))

To avoid installation problems, package version is not used for actual installation (in spite of the fact that its official 
convention, it doesnt work in some cases). For example, module above will be installed as (no  `-0.9`):

```bash
pip install git+https://github.com/ictxiangxin/boson/@b52727f7170acbedc5a1b4e1df03972bd9bb85e3#egg=boson
```

All pip supported vcs could be used: git, svn, hg, bzr 

If up-to-date logic, implemented by `pipInstall` task, does not suit your needs, you can always
disable it with `python.alwaysInstallModules = true` (pip always called). But this will be slower.

NOTE: since pip 20, compiled vcs module is cached (before it was build on each execution)

#### Virtualenv

When you declare any pip modules, plugin will try to use [virtualenv](https://virtualenv.pypa.io/en/stable/) 
in order to install required modules locally (for current project only).

If virtualenv is not installed - it will be installed automatically in `--user` scope. If you don't want automatic 
installation then disable it:

```groovy
python.installVirtualenv = false
```

In any case, plugin checks if virtualenv is already installed and use it to create local environment 
(if not, then fall back to  `--user` scope by default). Virtualenv usage is driven by declared scope, so if you don't want to use it set:

```groovy
python.scope = USER // or GLOBAL
```

With USER (or GLOBAL) scope, virtualenv will not be used, even if it's already created in project (plugin will ignore it and use global python).

If you already use virtualenv in your project (have created environment), then simply point plugin to use it:

```groovy
python.envPath = 'path/to/your/env'
```

It will automatically change `pythonPath` configuration accordingly.

NOTE: plugin will *not* create environment if you don't use any modules. If you still want to use project specific environment
(without declared pip modules) then create it manually: `python3 -m virtualenv .gradle/python` (default location). Plugin will recognize
existing env and use it.

IMPORTANT: virtualenv creates local python copy (by default in `.gradle/python`). Copy is created from global python and
later used *instead* of global python. If you want to change used python version in the environment, 
then manually remove `.gradle/python` so it could be created again (from global python).

To copy environment instead of symlinking (default) set ([--always-copy](https://virtualenv.pypa.io/en/stable/reference/#cmdoption-always-copy)):

```groovy
python.envCopy = true
```


#### Scope

Pip dependencies could be installed per project, for current user (~/) or globally.

Default behaviour:
 
* if [`virtualenv`](https://virtualenv.pypa.io/en/stable/) module installed (or automatically installed, see above): 
manage pip dependencies per project (env `.gradle/python` created)
* if no virtualenv - use user scope ([`--user`](https://pip.pypa.io/en/stable/user_guide/#user-installs) pip flag): 
pip modules are installed only for current user (this avoid permission problems on linux)

To change defaults:

```groovy
python.scope = VIRTUALENV
``` 

* `GLOBAL` - install modules globally (this may not work on linux due to permissions)
* `USER` - use `--user` flag to install for current user only
* `VIRTUALENV_OR_USER` - default
* `VIRTUALENV` - use `virtualenv` (if module not installed - error thrown)

Note that values may be declared without quotes because it's an enum which values are 
declared as project ext properties (`ext.USER==ru.vyarus.gradle.plugin.python.PythonExtension.Scope.USER`).

#### Check modules updates

To quick check if new versions are available for the registered pip modules
use `pipUpdates` task:

```text
:pipUpdates
The following modules could be updated:

	package            version latest type 
	------------------ ------- ------ -----
	click              6.6     6.7    wheel
```
 
Note that it will not show versions for transitive modules, only
for modules specified directly in `python.pip`.

To see all available updates (wihout filtering):

```groovy
pipUpdates.all = true
```

NOTE: If you see an error like 

```
TypeError: '>' not supported between instances of 'Version' and 'SetuptoolsVersion'
```

then [update pip](https://pip.pypa.io/en/stable/installing/#upgrading-pip):

```
pip install -U pip
```

#### Call python

Call python command:

```groovy
task cmd(type: PythonTask) {
    command = "-c print('sample')"
}
```

called: `python -c print('sample')` on win and `python -c exec("print('sample')")` on *nix (exec applied automatically for compatibility)

Call multi-line command:

```groovy
task cmd(type: PythonTask) {
    command = "-c \"import sys; print(sys.prefix)\""
}
```

called: `python -c "import sys; print(sys.prefix)"` on win and `python -c exec("import sys; print(sys.prefix)")` on *nix

NOTE: it is important to wrap script with space in quotes (otherwise parser will incorrectly parse arguments).

Call module:

```groovy
task mod(type: PythonTask) {
    module = 'sample' 
    command = "mod args"
}
```

called: `python -m sample mod args`

Call script:

```groovy
task script(type: PythonTask) { 
    command = "path/to/script.py 1 2"
}
```

called: `python path/to/script.py 1 2` (arguments are optional, just for demo)

String command is used for simplicity, but it could be array/collection of args:

```groovy
task script(type: PythonTask) { 
    command = ['path/to/script.py', '1', '2'] 
}
```

##### Command parsing

When command passed as string it is manually parsed to arguments array (split by space):

* Spaces in quotes are ignored: `"quoted space"` or `'quoted space'` 
* Escaped sapces are ignored: `with\\ space` (argument will be used with simple space then - escape removed).
* Escaped quotes are ignored: `"with \\"interrnal quotes\\" inside"`. But pay attention that it must be 2 symbols `\\"` and **not** `\"` because otherwise it is impossible to detect escape.  

To view parsed arguments run gradle with `-i` flag (enable info logs). In case when command can't be parsed properly 
(bug in parser or unsupported case) use array of arguments instead of string.

#### Configuration

##### Python location

On linux, plugin will use `python3` if available (and fall back to `python` if not). To use different binary use:

```groovy
python {
    pythonBinary = 'python'
}
```

This will force python 2 for linux. Also, this may be handy if python binary is named differently. 

To use non global python:

```groovy
python {
    pythonPath = 'path/to/python/binray/'
}
```

`pythonPath` must be set to directory containing python binary (e.g. 'path/to/python/binray/python.exe')

NOTE: `pythonPath` is ignored when virtualenv used.

##### Minimal python and pip versions

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

##### Pip

By default, all installed python modules are printed to console after pip installations 
using `pip list` (of course, if at least one module were declared for installation).
This should simplify problems resolution (show used transitive dependencies versions).

To switch off:

```groovy
python {
    showInstalledVersions = false
}
```

You can always see the list of installed modules with `pipList` task (exactly the same list as after pipInstall).

NOTE: if global python is used with USER scope and some modules were manually installed in global scope
then they will not be shown by pipList (and after pip install). To see all modules: 

```groovy
pipList.all = true
```

Global modules are hidden by default (for USER scope) because on linux there are a lot of system modules pre-installed.  
 
By default 'pip install' is not called for modules already installed with correct version.
In most situations this is preferred behaviour, but if you need to be sure about dependencies 
then force installation:

```groovy
python {
    alwaysInstallModules = true
}
```

##### Reference

All configuration options with default values:

```groovy
python {
   // path to python binary (global by default)
   pythonPath
   // python binary name (python or python3 by default)
   pythonBinary
   
   // minimal required python version (m.m.m)
   minPythonVersion
   // minimal required pip version (m.m.m)
   minPipVersion = '9'   
   
   // show all installed modules versions after pip installation
   showInstalledVersions = true
   // always call module install, even if correct version is already installed
   alwaysInstallModules = false
   
    // pip modules installation scope (project local, os user dir, global) 
   scope = VIRTUALENV_OR_USER
   // automatically install virtualenv module (if pip modules declared and scope allows)   
   installVirtualenv = true
   // used virtualenv path (if virtualenv used, see 'scope')
   envPath = '.gradle/python'
   // copy virtualenv instead of symlink (when created)
   envCopy = false
}
```

Note that in case of multi-module project envPath is set to '.gradle/python' inside the root project,
even if plugin is activated inside module (see [multi-module setup](https://github.com/xvik/gradle-use-python-plugin/wiki/Multi-module-projects)). 

#### PythonTask

PythonTask configuration:

| Property | Description |
|---------|--------------|
| pythonPath | Path to python binary. By default used path declared in global configuration |
| pythonBinary | Python binary name. By default, python3 on linux and python otherwise. |
| workDir | Working directory (important if called script/module do file operations). By default, it's a project root |
| createWorkDir | Automatically create working directory if does not exist. Enabled by default |
| module | Module name to call command on (if command not set module called directly). Useful for derived tasks. |
| command | Python command to execute (string, array, iterable) |
| logLevel | Logging level for python output. By default is `LIFECYCLE` (visible in console). To hide output use `LogLevel.INFO` |
| extraArgs | Extra arguments applied at the end of declared command. Useful for derived tasks to declare default options |
| outputPrefix | Prefix, applied for each line of python output. By default is '\t' to identify output for called gradle command |

Also, task provide extra method `extraArgs(String... args)` to declare extra arguments (shortcut to append values to 
extraArgs property).

#### PipInstallTask

Default pip installation task is registered as `pipInstall` and used to install modules, declared in global configuration. 
Custom task(s) may be used, if required:

```groovy
task myPipInst(type: PipInstallTask) {
    pip 'mod:1', 'other:2'
}
```

Configuration:

| Property | Description |
|----------|-------------|
| pythonPath | Path to python binary. By default used path declared in global configuration |
| pythonBinary | Python binary name. By default, python3 on linux and python otherwise. |
| modules | Modules to install. In most cases configured indirectly with `pip(..)` task methods. By default, modules from global configuration. |
| userScope | Use current user scope (`--user` flag). Enabled by default to avoid permission problems on *nix (global configuration). |
| showInstalledVersions | Perform `pip list` after installation. By default use global configuration value |
| alwaysInstallModules | Call `pip install module` for all declared modules, even if it is already installed with correct version. By default use global configuration value |

And, as shown above, custom methods: `pip(String... modules)` and `pip(Iterable<String> modules)`

### Use as base for specific module plugin

Plugin supposed to be used as base for plugins for specific python modules. With it you don't need 
to implement modules installation and could use provided abstractions to call python. 

Example usage: [gradle-mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin).

In your plugin, add plugin as dependency:

```groovy
dependencies {
    compile 'ru.vyarus:gradle-use-python-plugin:1.2.0'
}
```

And apply plugin: `project.plugins.apply(PythonPlugin)` (required to register `python` extension 
and declare default pipInstall task).

#### Extended task

The simplest way is to extend `PythonTask`:

```groovy
class SomeModuleTask extends PythonTask {
    
    @Override
    String getModule() {
        // always call specified commands on module
        return 'somemodule'
    }
    
    @Override
    List<String> getExtraArgs() {
        // example of module options configuration with custom extension 
        def res = []
        SomeModuleExtension ext = project.extensions.getByType(SomeModuleExtension)
        if (ext.somOption) {
            res << '--option'
        }
        return res
    }
    
    // optionally apply extra behaviour
    @Override
    void run() {
        // before python call               
        super.run()
        // after python call
    }
}
```

Usage: 

```groovy
pyton.pip 'sommemodule:1'

task modCmd(type: SomeModuleTask) {
    command = 'module args'
}
```

called: `python -m somemodule module arfs --option` 

In some cases, you can use `BasePythonTask` which is a super class of `PythonTask` and provides
only automatic `pythonPath` and `pythonBinary` properties set from global configuration. 

#### Completely custom task

Plugin provides `ru.vyarus.gradle.plugin.python.cmd.Python` utility class, which could be used directly in custom task 
(`PythonTask` is a wrapper above the utility).

Example usage:

```groovy
Python python = new Python(project, getPythonPath(), getPythonBinary())
            .logLevel(getLogLevel())
            .outputPrefix(getOutputPrefix())
            .workDir(getWorkDir())
            .extraArgs(getExtraArgs())

// execute and get command output
String out = python.readOutput(cmd)

// call module (the same as exec() but applies '-m mod' before command)
python.callModule('mod', cmd)

// direct python call
python.exec(cmd)
```

This could be used directly in the completely custom task.

Specific utility for target module could be defined, see 
`ru.vyarus.gradle.plugin.python.cmd.Pip` util as an example (simplified):

```groovy
class Pip {

    private final Python python

    Pip(Project project, String pythonPath, String binary) {
        // configure custom python execution util 
        python = new Python(project, pythonPath, binary)
                .logLevel(LogLevel.LIFECYCLE)
    }
    
    // declare module specific commands
    
    void install(String module) {
        python.callModule('pip', "install $module")
    }
}
```

#### Apply default modules

In your plugin you could apply default modules like this:

```groovy
afterEvaluate {
    PythonExtension ext = project.extensions.getByType(PythonExtension)
    // delayed default module(s) declaration based on user configuration
    if (!ext.isModuleDeclared('somemodule')) {
        ext.pip 'sommemodule:1'
    }
}
```

Or always declare default modules (before configuration):

```groovy
PythonExtension ext = project.extensions.getByType(PythonExtension)
ext.pip 'sommeodule:1', 'othermodule:2'
```

User will be able to override default versions by direct module declaration (even downgrade version):

```groovy
python.pip 'sommodule:0.9'
``` 

NOTE: all pip declarations are supported so direct module version could be overridden with VCS declaration
and vice-versa (only the declaration order is important).

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [pom-plugin](https://github.com/xvik/gradle-pom-plugin) - improves pom generation
* [java-lib-plugin](https://github.com/xvik/gradle-java-lib-plugin) - avoid boilerplate for java or groovy library project
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
