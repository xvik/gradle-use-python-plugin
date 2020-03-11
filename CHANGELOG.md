* (breaking) Drop java 7 support
* (breaking) Drop gradle 4 support
* Add `python.usePipCache` option to be able to disable cache for dependencies installation 
    ([--no-cache-dir](ttps://pip.pypa.io/en/stable/reference/pip_install/#caching))
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
* Add installed virtualenv version configuration: `python.virtualenv`. This way, plugin will 
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