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