# Welcome to gradle use-python plugin

!!! summary ""
    Use [python](https://www.python.org/) in gradle build. The only plugin intention is to simplify python usage from gradle (without managing python itself).

[Release notes](about/history.md) - [Compatibility](about/compatibility.md) - [License](about/license.md)

**[Who's using](https://github.com/xvik/gradle-use-python-plugin/discussions/18)**

## Features

* Works with [directly installed python](guide/python.md) or [docker container](guide/docker.md) (with python)
* Creates local (project-specific) [virtualenv](guide/configuration.md#virtualenv) (project-specific python copy)
* Installs required [pip modules](guide/modules.md) (venv by default, but could be global installation)
  - Support [requirements.txt](guide/modules.md#requirementstxt) file (limited by default)
* Gradle configuration cache supported 
* Could be used as basement for [building plugins](guide/plugindev.md) for specific python modules (like
  [mkdocs plugin](https://github.com/xvik/gradle-mkdocs-plugin))  

 