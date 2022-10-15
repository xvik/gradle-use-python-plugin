# Welcome to gradle use-python plugin

!!! summary ""
    Use [python](https://www.python.org/) in gradle build. The only plugin intention is to simplify python usage from gradle (without managing python itself).

[Release notes](about/history.md) - [Compatibility](about/compatibility.md) - [License](about/license.md)

## Features

* Creates local (project-specific) virtualenv (creating project-specific python copy)
* Installs required pip modules (virtualenv by default, but could be global installation)
* Could be used as basement for [building plugins](guide/plugindev.md) for specific python modules (like
  [mkdocs plugin](https://github.com/xvik/gradle-mkdocs-plugin))  

**[Who's using](https://github.com/xvik/gradle-use-python-plugin/discussions/18)**
 