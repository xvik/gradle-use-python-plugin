# Plugins development 

Plugin supposed to be used as base for plugins for specific python modules. With it, you don't need
to implement modules installation and could use provided abstractions to call python.

Example usage: [gradle-mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin).

In your plugin, add plugin as dependency:

```groovy
dependencies {
    implementation 'ru.vyarus:gradle-use-python-plugin:{{ gradle.version }}'
}
```

And apply plugin: `project.plugins.apply(PythonPlugin)` (required to register `python` extension
and declare default pipInstall task).

## Extended task

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

## Completely custom task

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

!!! important
    `Python` object use delayed initialization (to avoid putting all parameters inside
    constructor). By default, initialization will be performed automatically just before python command execution,
    but this also mean that system binary validation or other errors could happen at that point.
    It is often required to separate initialization and execution errors and so there is
    `.validate()` method triggering initialization (and, as a result, potential initialization errors).


## Apply default modules

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

!!! note 
    All pip declarations are supported so direct module version could be overridden with VCS declaration
    and vice-versa (only the declaration order is important).

## Hide sensitive data in logged command

By default, plugin always logs executed python commands, but sometimes such commands could
contain sensitive data (like passwords).

For example, pip's --extra-index-url may contain password:

```
--extra-index-url http://user:pass@something.com
```

In logged command password should be replaced with *****.

To deal with such cases, Python object supports registration of `LoggedCommandCleaner` object:

```java
python.logCommandCleaner(new CleanerInstance)
```

As an example see Pip object, which register special cleaner for extra index passwords right in its constructor:

```java
Pip(Python python, boolean userScope, boolean useCache) {
      ...
        
      // do not show passwords when external indexes used with credentials
      python.logCommandCleaner { CliUtils.hidePipCredentials(it) }
  }
```

See `CliUtils.hidePipCredentials` for an implementation example (using regexps).
Most likely, implementation would be the same in your case. 

## Call docker in task

In case of docker there might be a need to execute docker command directly.
For this, `BasePythonTask` contains `dockerExec` method.

For example, `CleanPython` task have to use it because on linux docker would create
environment as root user, and it would not be possible to remove it outside of docker:

```groovy
@TaskAction
void run() {
    String path = project.file(getEnvPath()).absolutePath
    if (dockerUsed) {
        // with docker, environment would be created with a root user and so it would not be possible
        // to simply remove folder: so removing within docker
        String[] cmd = windows ? ['rd', '/s', '/q', "\"$path\""] : ['rm', '-rf', path]
        if (dockerExec(cmd) != 0) {
            throw new GradleException('Python environment cleanup failed')
        }
    } else {
        project.delete(path)
    }
}
```

`isDockerUsed()`, `isWindows()` and `dockerExec(cmd)` are all provided by `BasePythonTask`

!!! note
    There is no way now to run on windows containers (due to testcontainers restriction),
    but plugin implements this support for the future.