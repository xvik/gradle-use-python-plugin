# Getting started

!!! note 
    Plugin **does not install python and pip** itself and use globally installed python (by default).
    It's easier to prepare python manually because python have good compatibility (from user perspective) and does not need to
    be updated often.

## installation

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-use-python-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-use-python-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/use-python/ru.vyarus.use-python.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.use-python)

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-use-python-plugin:{{ gradle.version }}'
    }
}
apply plugin: 'ru.vyarus.use-python'
```

OR

```groovy
plugins {
    id 'ru.vyarus.use-python' version '{{ gradle.version }}'
}
```

[Compatibility matrix](about/compatibility.md)

## Python & Pip

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

[Install python](guide/python.md) if required.

## Pip modules

If additional pip modules required configure them:

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

!!! important 
    Version ranges are not allowed for reproducible builds! (but, eventually, there would be problems
    in any case because of transitive dependencies)   

[Module features](guide/pip.md#pip-module-extra-features) and [VCS modules](guide/pip.md#vcs-pip-modules) supported.

## Behaviour

Default behaviour:

* if [`virtualenv`](https://virtualenv.pypa.io/en/stable/) module installed (or [automatically installed](guide/configuration.md#virtualenv)):
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

!!! note
    For multi-module projects, by default, plugin will create virtualenv inside the root project directory
    in order to share the same environment for all modules (but this [could be changed](guide/multimodule.md)).

## Usage

Call python command:

```groovy
task cmd(type: PythonTask) {
    command = "-c print('sample')"
}
```

called: `python -c print('sample')` on win and `python -c exec("print('sample')")` on *nix (exec applied automatically for compatibility)

!!! note
    Each `PythonTask` would depend on `checkPython` and `pipInstall` tasks which would
    prepare python environment before actual execution.

Call multi-line command:

```groovy
task cmd(type: PythonTask) {
    command = "-c \"import sys; print(sys.prefix)\""
}
```

called: `python -c "import sys; print(sys.prefix)"` on win and `python -c exec("import sys; print(sys.prefix)")` on *nix

!!! note
    It is important to wrap script with space in quotes (otherwise parser will incorrectly parse arguments).

See [command parsing specifics and env variables usage](guide/usage.md#command-parsing)

String command is used for simplicity, but it could be array/collection of args:

```groovy
task script(type: PythonTask) { 
    command = ['path/to/script.py', '1', '2'] 
}
```

### Module

```groovy
task mod(type: PythonTask) {
    module = 'sample' 
    command = "mod args"
}
```

called: `python -m sample mod args`

### Script

```groovy
task script(type: PythonTask) { 
    command = "path/to/script.py 1 2"
}
```

called: `python path/to/script.py 1 2` (arguments are optional, just for demo)