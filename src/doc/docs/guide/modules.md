# Pip modules

If additional pip modules required:

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

Module format is: `name:version` (will mean `name==version` in pip notion). Non-strict version definition is not allowed (for obvious reasons).
Dependencies are installed in declaration order. If duplicate declaration specified then only the latest declaration
will be used:

```groovy
python.pip 'module1:2.0', 'module2:1.0', 'module1:1.0' 
```

Will install version 1.0 of module1 because it was the latest declaration. "Module overrides"
works for all declaration types (see below): the latest declared module version always wins.

Dependencies are installed with `pipInstall` task which is called before any declared `PythonTask`.

By default, dependencies are installed inside project specific virtualenv (project specific copy of python environment, 
configured with `python.envPath`).

Behaviour matrix for possible `scope` and `installVirtualenv` configurations:

scope | installVirtualenv | Behaviour | default
------|-----|------ |-----
GLOBAL | ignored | packages installed in global scope (`pip install name`)|
USER | ignored | packages installed in user scope (`pip install name --user`)|
VIRTUALENV_OR_USER | true | if virtualenv not installed, install it in user scope; create project specific virtualenv and use it | default
VIRTUALENV_OR_USER | false | when virtualenv is not installed install packages in user scope (same as USER); when virtualenv installed create project specific virtualenv and use it |
VIRTUALENV | true | if virtualenv not installed, install it in user scope; create project specific virtualenv and use it |
VIRTUALENV | false | throw error when virtualenv not installed 

Note that `VIRTUALENV + true` and `VIRTUALENV_OR_USER + true` behaviours are the same. Different scope
name here describes behavior for unexpected `installVirtualenv=false` change (to fail or fallback to user scope).

`USER` and `GLOBAL` scopes will ignore local (virtual)environment, even if project-specific environment was created before,
with these options global python will be used instead.  

## Pip module extra features

You can declare modules with [extra features](https://setuptools.readthedocs.io/en/latest/setuptools.html#declaring-extras-optional-features-with-their-own-dependencies)
in module name to install special version of module (with enabled features):

```groovy
python.pip 'requests[socks,security]:2.18.4'
```

!!! important
    It is impossible to track if this "variation" of module is installed, so
    plugin performs up-to-date check for such modules by name only (for example,
    if 'requests==2.18.4' is already installed). For most cases, this is suitable behaviour because, by default,
    modules are installed in virtualenv and so you will always have correct module installed.
    For other cases, you can disable up-to-date checks (delegate all dependencies logic to pip): `python.alwaysInstallModules = true`

## VCS pip modules

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

!!! warning 
    Module version part assumed to follow the last dash, so if you specify version like
    `somethinf-12.0-alpha.1` it would be parsed incorrectly (as package `somethinf-12.0` version `alpha.1`)!
    Don't use dashes in version!

Vcs module installation is: source checkout and module build (using setup.py). You may need to specify subdirectory
as `&subdirectory=pkg_dir` ([see docs](https://pip.pypa.io/en/stable/reference/pip_install/#vcs-support))

To avoid installation problems, package version is not used for actual installation (in spite of the fact that its official
convention, it doesn't work in some cases). For example, module above will be installed as (without `-0.9`):

```bash
pip install git+https://github.com/ictxiangxin/boson/@b52727f7170acbedc5a1b4e1df03972bd9bb85e3#egg=boson
```

All pip supported vcs could be used: git, svn, hg, bzr

If up-to-date logic, implemented by `pipInstall` task, does not suit your needs, you can always
disable it with `python.alwaysInstallModules = true` (pip always called). But this will be slower.

!!! note 
    Since pip 20, compiled vcs module is cached (before it was build on each execution), but
    it is possible to disable cache (for all modules) with `python.usePipCache=false` configuration
    (applies [--no-cache-dir](https://pip.pypa.io/en/stable/reference/pip_install/#caching) pip flag)

## Extra pip repositories

To add additional pip repositories (probably self-hosted):

```groovy
python {
    extraIndexUrls = ["http://extra-url.com", "http://extra-url.com"]
}
```

or with shortcut method (shortcut may be used multiple times):

```groovy
python {
    extraIndexUrls "http://extra-url.com", "http://extra-url2.com" 
}
```

Extra urls will be applied as [--extra-index-url](https://pip.pypa.io/en/stable/reference/pip_install/#install-extra-index-url)
flag for pip commands supporting it: install, download, list and wheel. By default, it only affects `pipInstall` and `pipList` tasks.
Applied for all `BasePipTask`, so if you have custom pip tasks, it would be affected too.

In case of ssl problems (stale or self-signed certificated), mark domains as trusted:

```groovy
python {
    trustedHosts = ["extra-url.com"]
}
```

or

```groovy
python {
    trustedHosts "extra-url.com"
}
```

Applied as [--trusted-host](https://pip.pypa.io/en/stable/reference/pip/#trusted-host)
option only for `pipInstall` (because `pip install` is the only command supporting this option).

!!! note
    If, for some reason, you don't want to specify it for all pip tasks, you can configure exact task,
    for example: `pipInstall.extraIndexUrls = ["http://extra-url.com", "http://extra-url2.com"]`

## Extra pip install options

It is impossible to support directly all possible `pip install` [options](https://pip.pypa.io/en/stable/reference/pip_install/#options)
usages directly with api (safe way), so there is a direct configuration for an additional options. For example:

```groovy
pipInstall.options('--upgrade-strategy', 'only-if-needed')
```

Shortcut method above may be called multiple times:

```groovy
pipInstall.options('--a', 'value')
pipInstall.options('--b', 'value')
```

Or you can use property directly:

```groovy
pipInstall.options = ['--a', 'value', '--b', 'value']
```

## Requirements.txt

Plugin supports python requirements.txt file: if file found in project root, it would
be loaded automatically.

To specify different file location:

```groovy
python.requirements.file = 'path/to/file' // relative to project root
```

To switch off requirements support:

```groovy
python.requirements.use = false
```

!!! note
    In multi-module projects file is searched relatively to current module. Root module is not
    searched to avoid situation when root file used in module by mistake.

    If required, search in root could be configured manually: 
    ```groovy
    python.requirements.file = project.rootProject.rootDir.absolutePath
    ```

### Strict mode

By default, restricted file syntax assumed: 

* Support exactly the same module types as in gradle declaration:
  - Only strict module version (e.g. `foo==1.0`)
  - With [features](#pip-module-extra-features) support
  - [VCS modules](#vcs-pip-modules) with extended syntax (including version)
    * This syntax might not be parsed correctly by python tools,
      but it is required by plugin in order to know installed version (and properly perform up-to-date check).
* All commented or empty lines are skipped

!!! note "Motivation"
    Allow externalizing pip modules configuration file so
    python tools could see and parse it, but still restrict version ranges (for reproducible builds).
    As en example, depndabot could auto update module versions. 

Example file:

```
# simple module (exact version)
extract-msg == 0.34.3

# features
requests[socks,security] == 2.28.1

# vcs syntax (with version part!)
git+https://github.com/ictxiangxin/boson/@ea7d9113f71a7eb79083208d4f3bbb74feeb149f#egg=boson-1.4
```

In this mode requirements file read by plugin itself and registered in gradle modules
(the same as if modules were declared directly in gradle file).

!!! important
    Module declarations in gradle script override requirements declaration. So if, for example,
    requirements contains `foo==1.1` and in gradle script `python.pip 'foo:1.0'` then version 1.0 would be used.

### Native behaviour

You can also use requirement file in a [native way](https://pip.pypa.io/en/stable/reference/requirements-file-format/#requirements-file-format):

```groovy
python.requirements.strict = false
```

In this case instead of manual file parsing plugin will delegate processing to pip:

```
pip -r requirements.txt
```

!!! note
    Plugin up-to-date check will rely on requirements file last edit time (because plugin not aware of modules inside it).
    `pipUpdates` task will not show updates for modules in requirements file (but you could 
    configure it to show all modules `pipUpdates.all = true`)

If modules also declared in gradle file directly, they would be installed **after** requirements processing.

As an example, this mode might be helpful if you need to rely on python modules, built in gradle's project submodules
(in this case python task dependencies must be properly set).

See [requirements file syntax](https://pip.pypa.io/en/stable/reference/requirements-file-format/#requirements-file-format) for all available options

## Scope

Pip dependencies could be installed per project, for current user (~/) or globally.

Default behaviour:

* if [`virtualenv`](https://virtualenv.pypa.io/en/stable/) module installed (or [automatically installed](configuration.md#virtualenv)):
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
    Values may be declared without quotes because it's an enum which values are
    declared as project ext properties (`ext.USER==ru.vyarus.gradle.plugin.python.PythonExtension.Scope.USER`).

Complete behaviour matrix [see above](#).

## Check modules updates

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

To see all available updates (without filtering):

```groovy
pipUpdates.all = true
```

!!! note
  If you see an error like

  ```
  TypeError: '>' not supported between instances of 'Version' and 'SetuptoolsVersion'
  ```

  then [update pip](https://pip.pypa.io/en/stable/installing/#upgrading-pip):

  ```
  pip install -U pip
  ```

## Cleanup environment

Use `cleanPython` task to remove current project-specific python environment.

This would be required for python version change and for switching to docker (or back). 
 