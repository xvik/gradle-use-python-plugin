# Stats

Plugin records all external command executions and could show you report after the build.

To enable report:

```groovy
python.printStats = true
```

!!! note
    Report includes **all** external commands, including hidden commands, used by plugin
    for python and other modules detection.

## Example

For example, for a sample project like this:

```groovy
python {
    pip 'extract-msg:0.28.0'
    
    printStats = true 
}

tasks.register('sample', PythonTask) {
    command = '-c print(\'samplee\')'
}
```

Output would look like this (for `sample` task execution):

```
Python execution stats:

task                                        started         duration            
:checkPython                                18:18:51:117    59ms                python3 --version
:checkPython                                18:18:51:193    32ms                python3 -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:checkPython                                18:18:51:241    213ms               python3 -m pip --version
:checkPython                                18:18:51:469    47ms                python3 -m venv -h
:checkPython                                18:18:51:521    2.34s               python3 -m venv .gradle/python
:checkPython                                18:18:53:859    9ms                 .gradle/python/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:checkPython                                18:18:53:870    217ms               .gradle/python/bin/python -m pip --version
:pipInstall                                 18:18:54:113    300ms               .gradle/python/bin/python -m pip freeze
:pipInstall                                 18:18:54:417    3.19s               .gradle/python/bin/python -m pip install extract-msg==0.28.0
:pipInstall                                 18:18:57:612    289ms               .gradle/python/bin/python -m pip list --format=columns
:sample                                     18:18:57:905    9ms                 .gradle/python/bin/python -c exec("print('samplee')")

    Executed 11 commands in 6.7s (overall)
```

## Failed tasks

Failed tasks are also indicated in project.

For example, for project:

```groovy
python {               
    printStats = true
}

tasks.register('sample', PythonTask) {
    // ERROR in syntax
    command = '-c printt(\'samplee\')'
}
```

Stats would be:

```
Python execution stats:

task                                        started         duration            
:checkPython                                18:22:35:383    50ms                python3 --version
:checkPython                                18:22:35:445    32ms                python3 -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:sample                                     18:22:35:512    37ms       FAILED   python3 -c exec("printt('samplee')")

    Executed 3 commands in 119ms (overall)
```

## Docker

For docker, executions are also tracked: 

```groovy
python {
    pip 'extract-msg:0.28.0'
    docker.use = true
    printStats = true
}

tasks.register('sample', PythonTask) {
    command = '-c print(\'samplee\')'
}
```

Note that docker container name in shown in report:

```
Python execution stats:

task                                        started        docker container     duration            
:checkPython                                18:25:43:159   /cool_noyce          2.08s               python3 --version
:checkPython                                18:25:45:243   /cool_noyce          33ms                python3 --version
:checkPython                                18:25:45:288   /cool_noyce          60ms                python3 -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:checkPython                                18:25:45:362   /cool_noyce          1.09s               python3 -m pip --version
:checkPython                                18:25:46:466   /cool_noyce          106ms               python3 -m venv -h
:checkPython                                18:25:46:576   /cool_noyce          53ms       FAILED   test -f /usr/local/bin/activate && echo "exists"
:checkPython                                18:25:46:630   /cool_noyce          2.81s               python3 -m venv .gradle/python
:checkPython                                18:25:49:443   /cool_noyce          70ms                .gradle/python/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:checkPython                                18:25:49:516   /cool_noyce          269ms               .gradle/python/bin/python -m pip --version
:pipInstall                                 18:25:49:886   /cool_noyce          356ms               .gradle/python/bin/python -m pip freeze
:pipInstall                                 18:25:50:246   /cool_noyce          6s                  .gradle/python/bin/python -m pip install extract-msg==0.28.0
:pipInstall                                 18:25:56:316   /cool_noyce          351ms               .gradle/python/bin/python -m pip list --format=columns
:sample                                     18:25:56:674   /cool_noyce          90ms                .gradle/python/bin/python -c exec("print('samplee')")

    Executed 13 commands in 13.36s (overall)
```

Note that report also includes `test -f /usr/local/bin/activate && echo "exists"` command
which is not python execution, but used for file detection inside container (stats show all executions).

## Multi-module projects

In multi-module project, report would contain stats for all modules:

```
Python execution stats:

task                                        started         duration            
:checkPython                                18:28:43:049    42ms                python3 --version
:mod12:checkPython                          18:28:43:101    16ms                ../env/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod12:checkPython                          18:28:43:128    498ms               ../env/bin/python -m pip --version
:mod12:checkPython                          18:28:43:640    31ms                ../env/bin/python -m venv -h
:mod12:checkPython                          18:28:43:676    2.23s               ../env/bin/python -m venv ../.gradle/python
:mod1:checkPython                           18:28:45:908    10ms                ../.gradle/python/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod12:checkPython                          18:28:45:923    238ms               ../.gradle/python/bin/python -m pip --version

    Executed 7 commands in 3.06s (overall)
```

!!! important
    In multi-module project, only root module's extension configuration `printStats = true`
    would enable stats (simply because stats are global and can't be configured per-project).
     
    Or it would be the top-most sub-project applying python plugin.

## Duplicates detection

The report also groups duplicate commands automatically.

For example, if each sub-module initializes its own virtualenv:

```groovy
subprojects {
    // NOTE each module has its own virtualenv      
    python.envPath = 'envx'
}
```

Report would be:

```
Python execution stats:

task                                        started         duration            
:checkPython                                19:15:18:011    50ms                python3 --version
:mod4:checkPython                           19:15:18:074    16ms                ../env/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod4:checkPython                           19:15:18:103    511ms               ../env/bin/python -m pip --version
:mod4:checkPython                           19:15:18:629    30ms                ../env/bin/python -m venv -h
:mod4:checkPython                           19:15:18:664    2.25s               ../env/bin/python -m venv envx
:checkPython                                19:15:20:912    28ms                python3 -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod4:checkPython                        || 19:15:20:943    11ms                envx/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod3:checkPython                        || 19:15:20:946    2.34s               ../env/bin/python -m venv envx
:mod4:checkPython                        || 19:15:20:958    243ms               envx/bin/python -m pip --version
:mod4:pipInstall                         || 19:15:21:215    276ms               envx/bin/python -m pip freeze
:mod4:pipInstall                         || 19:15:21:495    1.53s               envx/bin/python -m pip install extract-msg==0.28.0
:mod4:pipInstall                         || 19:15:23:023    306ms               envx/bin/python -m pip list --format=columns
:mod3:checkPython                        || 19:15:23:285    11ms                envx/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod1:checkPython                        || 19:15:23:287    2.38s               ../env/bin/python -m venv envx
:mod3:checkPython                        || 19:15:23:299    241ms               envx/bin/python -m pip --version
:mod3:pipInstall                         || 19:15:23:547    275ms               envx/bin/python -m pip freeze
:mod3:pipInstall                         || 19:15:23:824    1.89s               envx/bin/python -m pip install extract-msg==0.28.0
:mod1:checkPython                        || 19:15:25:666    10ms                envx/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod2:checkPython                        || 19:15:25:669    2.38s               ../env/bin/python -m venv envx
:mod1:checkPython                        || 19:15:25:678    244ms               envx/bin/python -m pip --version
:mod3:pipInstall                         || 19:15:25:712    327ms               envx/bin/python -m pip list --format=columns
:mod1:pipInstall                         || 19:15:25:929    280ms               envx/bin/python -m pip freeze
:mod1:pipInstall                         || 19:15:26:211    1.58s               envx/bin/python -m pip install extract-msg==0.28.0
:mod1:pipInstall                         || 19:15:27:793    308ms               envx/bin/python -m pip list --format=columns
:mod2:checkPython                        || 19:15:28:054    9ms                 envx/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod5:checkPython                        || 19:15:28:056    2.38s               ../env/bin/python -m venv envx
:mod2:checkPython                        || 19:15:28:065    239ms               envx/bin/python -m pip --version
:mod2:pipInstall                         || 19:15:28:312    268ms               envx/bin/python -m pip freeze
:mod2:pipInstall                         || 19:15:28:581    1.57s               envx/bin/python -m pip install extract-msg==0.28.0
:mod2:pipInstall                         || 19:15:30:155    311ms               envx/bin/python -m pip list --format=columns
:mod5:checkPython                        || 19:15:30:434    9ms                 envx/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)")
:mod5:checkPython                        || 19:15:30:445    224ms               envx/bin/python -m pip --version
:mod5:pipInstall                            19:15:30:675    268ms               envx/bin/python -m pip freeze
:mod5:pipInstall                            19:15:30:944    1.56s               envx/bin/python -m pip install extract-msg==0.28.0
:mod5:pipInstall                            19:15:32:505    300ms               envx/bin/python -m pip list --format=columns

    Executed 35 commands in 24.64s (overall)

    Duplicate executions:

		../env/bin/python -m venv envx (5)
			:mod4:checkPython   (work dir: mod4)
			:mod3:checkPython   (work dir: mod3)
			:mod1:checkPython   (work dir: mod1)
			:mod2:checkPython   (work dir: mod2)
			:mod5:checkPython   (work dir: mod5)

		envx/bin/python -c exec("import sys;ver=sys.version_info;print(str(ver.major)+'.'+str(ver.minor)+'.'+str(ver.micro));print(sys.prefix);print(sys.executable)") (5)
			:mod4:checkPython   (work dir: mod4)
			:mod3:checkPython   (work dir: mod3)
			:mod1:checkPython   (work dir: mod1)
			:mod2:checkPython   (work dir: mod2)
			:mod5:checkPython   (work dir: mod5)

		envx/bin/python -m pip --version (5)
			:mod4:checkPython   (work dir: mod4)
			:mod3:checkPython   (work dir: mod3)
			:mod1:checkPython   (work dir: mod1)
			:mod2:checkPython   (work dir: mod2)
			:mod5:checkPython   (work dir: mod5)

		envx/bin/python -m pip freeze (5)
			:mod4:pipInstall   (work dir: mod4)
			:mod3:pipInstall   (work dir: mod3)
			:mod1:pipInstall   (work dir: mod1)
			:mod2:pipInstall   (work dir: mod2)
			:mod5:pipInstall   (work dir: mod5)

		envx/bin/python -m pip install extract-msg==0.28.0 (5)
			:mod4:pipInstall   (work dir: mod4)
			:mod3:pipInstall   (work dir: mod3)
			:mod1:pipInstall   (work dir: mod1)
			:mod2:pipInstall   (work dir: mod2)
			:mod5:pipInstall   (work dir: mod5)

		envx/bin/python -m pip list --format=columns (5)
			:mod4:pipInstall   (work dir: mod4)
			:mod3:pipInstall   (work dir: mod3)
			:mod1:pipInstall   (work dir: mod1)
			:mod2:pipInstall   (work dir: mod2)
			:mod5:pipInstall   (work dir: mod5)
```

!!! important
    Pay attention that report does not count execution context for command:
    for example `envx/bin/python -m pip --version` executed multiple times
    because each module has its own environment, stored at the same directory.
    So, essentially, it is different commands! 

!!! note
    Note '||' symbol near some commands - it indicates parallel execution with one
    or more other python commands (this build was started with `--parallel` flag).