### 2.2.0 (2020-04-06)
* Add support for `pip` flags: [--trusted-host](https://pip.pypa.io/en/stable/reference/pip/#trusted-host) 
  and [--extra-index-url](https://pip.pypa.io/en/stable/reference/pip_install/#install-extra-index-url) (#10)
  May be set in `python` extension or directly for `pip` tasks (`extraIndexUrls`, `trustedHosts`). 
  Flags applied only to compatible pip commands.
* Allow dashes in vcs module name (for example, now it is possible to specify `#egg=my-module-11.2`).
  NOTE: This may lead to problems with versions also containing dashes (1.1-alpha.1), but
  it may be easily changed manually (to version without dashes: 1.1.alpha.1)
* Move python configuration options from `PythonTask` to `BasePythonTask`:
  `pythonArgs`, `environment`, `workDir`, `logLevel`. Now pip tasks could use 
  these options to fine tune python execution (under pip call).
  NOTE: extraArgs was not moved because exact tasks (like pipInstall) could perform
  multiple commands calls and applying args to all of them is not correct (most likely, fail the build).
  Instead, tasks must implement their own support for additional args.
* Add free options to `pipInstall`:  
  `pipInstall.options('--upgrade-strategy', 'only-if-needed')`.
  It is not possible to support every possible pip flag with api so this manual customization
  is required to cover wider range of use-cases.
* Fix gradle deprecation warnings on some tasks properties (#9)
* Add environment variables configuration in extension: `python.environment 'SAMPLE', 'value'`
* Fix checkPython execution when running from daemon (gradle work dir may differ from project root:
  confirmed case with gradle 6 on java 11).
* Use relative path to virtualenv when possible instead of always absolute          

### 2.1.0 (2020-03-17)
* Add environment variables support (#8):
    - Python object: `Python.environment` (single and map)
    - Python task: `PythonTask.environment` (single and map)

### 2.0.0 (2020-03-13)
* (breaking) Drop java 7 support
* (breaking) Drop gradle 4 support
* Add `python.usePipCache` option to be able to disable cache for dependencies installation 
    ([--no-cache-dir](https://pip.pypa.io/en/stable/reference/pip_install/#caching))
  Also, option added to Pip object constructor and BasePipTask (with default from extension)
* Add `Python.getBinaryDir()` returning (in most cases) executed python binary folder
    (based on `sys.executable` with fallback to `sys.prefix/bin`)
* Fix virtualenv installation fail when "global" python is already virtualenv due to --user flag usage 
  (like on travis now)
    - Add `Python.isVirtualenv()` method. Virtualenv is detected by `activate` script in python binary 
      (which may not be always accurate, but should work in the majority of cases)
    - `Pip` internally use `Python.isVirtualenv()` to prevent applying --user flag 
    - Configured (`envPath`) or created virtualenv is validated (shows error on incorrect configuration)
    - Show warnings for cases when virtualenv is created from another virtualenv (because it may have side effects)
* Add python extra args support (in contrast to extra agrs, applied after command, python args applied before):
    - Add `Python.pythonArgs` -  args applied just after python executable
    - Add `PythonTask.pythonArgs`   
* Add `Virtualenv.python` accessor to be able to configure additional arguments
* Add installed virtualenv version configuration: `python.virtualenvVersion`. This way, plugin will 
    always install only known to be working version and avoid side effects of "just released" 
    versions. By default, 16.7.9 would be installed because 20.0.x has some not fixed regressions
* Use gradle tasks configuration avoidance for lazy tasks initialization (no init when tasks not needed)                    

### 1.2.0 (2018-08-30)
* Improve virtualenv usage in multi-module project (#5):
    - Fix virtualenv access from module on windows
    - By default virtualenv is created in the root project and used for all modules
        (breaking) note that before virtualenv was created inside module (on linux)
    - Per module python setup is also possible by overriding envPath

### 1.1.0 (2018-05-29)
* Add vcs modules support in format: "vcs+protocol://repo_url/@vcsVersion#egg=pkg-pkgVersion" (#2)
* Support module features syntax: module\[feature]:version (#3)
* PipModule api changes:
    - Added toPipInstallString(): must be used for installation command instead of toPipString()  

### 1.0.2 (2018-04-18)
* Fix pip 10 compatibility (#1)

### 1.0.1 (2017-12-26)
* Fix python command recognition: avoid wrapping for -c module argument
* Support escaped space and quote during command string parsing
* Support command definition as array/list in PythonTask 

### 1.0.0 (2017-12-20)
* Initial release