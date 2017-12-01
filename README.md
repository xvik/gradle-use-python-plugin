# Gradle use-python plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-use-python-plugin.svg)](https://travis-ci.org/xvik/gradle-use-python-plugin)

### About

Plugin **does not install python and pip** itself (because it's easier to do it manually and python have great 
compatibility so does not need to be installed often): by default, globally installed python used.

The only plugin intention is to simplify python usage from gradle.

Features:
* Install required python modules (guarantee exact versions, even if newer installed) using pip 
* Provides task to call python commands, modules or scripts (PythonTask)
* Could be used as basement for building plugins for specific python modules (like 
[mkdocs plugin](https://github.com/xvik/gradle-mkdocs-plugin))

##### Summary

* Configuration: `python`
* Tasks:
    - `pipInstall` - install declared pip modules
    - `pipUpdates` - show the latest available versions for the registered modules
    - `type:PythonTask` - call python command/script/module
    - `type:PipInstallTask` - may be used for custom pip modules installation workflow


### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-use-python-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-use-python-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.use-python).


[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-use-python-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-use-python-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-use-python-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-use-python-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-use-python-plugin:1.0.0'
    }
}
apply plugin: 'ru.vyarus.use-python'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.use-python' version '1.0.0'
}
```

#### Python & Pip

Make sure python is installed:

```bash
python --version
```

On most *nix distributions python is already installed. On windows 
[download and install](https://www.python.org/downloads/windows/) python manually or use 
[chocolately](https://chocolatey.org/packages/python/3.6.3) (`choco install python`)

Pip is also assumed to be installed.

```bash
pip --version
```

##### Automatic python install

I assume that automatic python management is important for python dev, but not for python modules usage.
Install once (like java) and forget works perfectly.

If you need automatic python installation, look JetBrain's 
[python-envs plugin](https://github.com/JetBrains/gradle-python-envs) (note that on windows python could be 
installed automatically just once and requires manual un-installation). 

Another option is to use
[pythonenv](https://packaging.python.org/guides/installing-using-pip-and-virtualenv/) 
(or [pipenv](https://docs.pipenv.org/)) to separate
different projects packages. You can even use custom `PythonTask`'s to automate virtualenv management and automatic switching.

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

Module format is: `name:version` (will mean "name==version" in pip notion). Non strict version definition is not allowed (for obvious reasons).
Dependencies are installed in declaration order. If duplicate declaration specified then only the latest declaration
will be used:

```groovy
python.pip 'module1:2.0', 'module2:1.0', 'module1:1.0' 
```

Will install version 1.0 of module1 because it was the latest declaration.

Dependencies are installed with `pipInstall` task, called before any declared `PythonTask`.

#### Check modules updates

To quick check if new versions are available for the registered pip modules
use `pipUpdates` task:

```
:pipUpdates
The following modules could be updated:

	package            version latest type 
	------------------ ------- ------ -----
	click              6.6     6.7    wheel
```
 
Note that it will not show versions for transitive modules, only
for modules specified directly in `python.pip`.

#### Call python

Call python command:

```groovy
task cmd(type: PythonTask) {
    command = "-c print('sample')"
}
```

called: `python -c print('sample')`

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

#### Configuration

##### Python location

To use non global python:

```groovy
python {
    pythonPath = 'path/to/python/binray/'
}
```

`pythonPath` must be set to directory containing python binary (e.g. 'path/to/python/binray/python.exe')

##### Minimal python version

To set python version constraint:

```groovy
python {
    minVersion = '3.2'
}
```

Python version format is: major.minor.micro.
Constraint may include any number of levels: '3', '3.1', '2.7.5'

##### Pip

By default, all installed python modules are printed to console after pip installations 
using `pip list` (of course, if at least one module were declared for installation).
This should simplify problems resolution.

To switch off:

```groovy
python {
    showInstalledVersions = false
}
```
 
Also, by default 'pip install' is not called for modules already installed with correct version.
In most situations this is preferred behaviour, but if you need to be sure about dependencies 
then force installation:

```groovy
python {
    alwaysInstallModules = true
}
```

#### PythonTask

PythonTask configuration:

| Property | Description |
|---------|--------------|
| pythonPath | Path to python binary. By default used path declared in global configuration |
| workDir | Working directory (important if called script/module do file operations). By default, it's a project root |
| createWorkDir | Automatically create working directory if does not exist. Enabled by default |
| module | Module name to call command on (if command not set module called directly). Useful for derived tasks. |
| command | Python command to execute |
| logLevel | Logging level for python output. By default is `LIFECYCLE` (visible in console). To hide output use `LogLevel.INFO` |
| extraArgs | Extra arguments applied at the end of declared command. Useful for derived tasks to declare default options |
| outputPrefix | Prefix, applied for each line of python output. By default is '\t' to identify output for called gradle command |

Also, task provide extra method `extraArgs(String... args)` to declare extra arguments (shortcut to append values to 
extraArgs property).

#### PipInstallTask

Default pip installation task is registered as `pipInstall' and used to install modules, declared in global configuration. 
Custom task(s) may be used, if required:

```groovy
task myPipInst(type: PipInstallTask) {
    pip 'mod:1', 'other:2'
}
```

Configuration:

| Property | Description |
|----------|-------------|
| minPythonVersion | Minimal required python version. By default used version declared in global configuration |
| pythonPath | Path to python binary. By default used path declared in global configuration |
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
    compile 'ru.vyarus:gradle-use-python-plugin:1.0.0'
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

#### Completely custom task

Plugin provides `ru.vyarus.gradle.plugin.python.cmd.Python` utility class, which could be used directly in custom task 
(`PythonTask` is a wrapper above the utility).

Example usage:

```groovy
Python python = new Python(project, getPythonPath())
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
`ru.vyarus.gradle.plugin.python.cmd.Pip` util as an example:

```groovy
class Pip {

    private final Python python

    Pip(Project project, String pythonPath) {
        // configure custom python execution util 
        python = new Python(project, pythonPath)
                .logLevel(LogLevel.LIFECYCLE)
    }
    
    // declare module specific commands
    
    void install(String module) {
        exec("install $module")
    }
}
```

#### Apply default modules

In your plugin you could apply default modules like this:

```groovy
afterEvaluate {
    PythonExtension ext = project.extensions.getByType(PythonExtension)
    // declare default module(s) if it was not declared by user manually
    if (!ext.isModuleDeclared('somemodule')) {
        ext.pip 'sommemodule:1'
    }
}
```

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [pom-plugin](https://github.com/xvik/gradle-pom-plugin) - improves pom generation
* [java-lib-plugin](https://github.com/xvik/gradle-java-lib-plugin) - avoid boilerplate for java or groovy library project
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
