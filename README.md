# Gradle use-python plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/gradle-use-python-plugin/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/gradle-use-python-plugin/actions/workflows/CI.yml)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-use-python-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-use-python-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-use-python-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-use-python-plugin)

**DOCUMENTATION**: https://xvik.github.io/gradle-use-python-plugin/

### About

Plugin **does not install python and pip** itself and use globally installed python (by default). 
It's easier to prepare python manually because python have good compatibility (from user perspective) and does not need to 
be updated often.

Also, plugin could run python inside docker container to avoid local python installation.

The only plugin intention is to simplify python usage from gradle. By default, plugin creates python virtualenv
inside the project and installs all modules there so each project has its own python (copy) and could not be 
affected by other projects or system changes.

Features:

* Works with directly installed python or docker container (with python)
* Creates local (project-specific) virtualenv (project-specific python copy)
* Installs required pip modules (virtualenv by default, but could be global installation) 
* Support requirements.txt file (limited by default)
* Could be used as basement for building plugins for specific python modules (like 
[mkdocs plugin](https://github.com/xvik/gradle-mkdocs-plugin))

**[Who's using (usage examples)](https://github.com/xvik/gradle-use-python-plugin/discussions/18)**

##### Summary

* Configuration: `python`
* Tasks:
    - `checkPython` - validate python installation (and create virtualenv if required)
    - `cleanPython` - clean created python environment
    - `pipInstall` - install declared pip modules
    - `pipUpdates` - show the latest available versions for the registered modules
    - `pipList` - show all installed modules (the same as pipInstall shows after installation)
    - `type:PythonTask` - call python command/script/module
    - `type:PipInstallTask` - may be used for custom pip modules installation workflow

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-use-python-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-use-python-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/use-python/ru.vyarus.use-python.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.use-python)

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-use-python-plugin:3.0.0'
    }
}
apply plugin: 'ru.vyarus.use-python'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.use-python' version '3.0.0'
}
```  

#### Compatibility

Plugin compiled for java 8, compatible with java 11. Supports python 2 and 3 on windows and linux (macos).

Gradle | Version
--------|-------
5.3-7     | 3.0.0
5-5.2     | 2.3.0
4.x     | [1.2.0](https://github.com/xvik/gradle-use-python-plugin/tree/1.2.0)

#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#ru.vyarus/gradle-use-python-plugin)
* Select `Commits` section and click `Get it` on commit you want to use 
    or use `master-SNAPSHOT` to use the most recent snapshot

For gradle before 6.0 use `buildscript` block with required commit hash as version:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'ru.vyarus:gradle-use-python-plugin:2450c7e881'
    }
}
apply plugin: 'ru.vyarus.use-python'
```

For gradle 6.0 and above:

* Add to `settings.gradle` (top most!) with required commit hash as version:

  ```groovy
  pluginManagement {
      resolutionStrategy {
          eachPlugin {
              if (requested.id.namespace == 'ru.vyarus.use-python') {
                  useModule('ru.vyarus:gradle-use-python-plugin:2450c7e881')
              }
          }
      }
      repositories {
          maven { url 'https://jitpack.io' }
          gradlePluginPortal()          
      }
  }    
  ``` 
* Use plugin without declaring version: 

  ```groovy
  plugins {
      id 'ru.vyarus.use-python'
  }
  ```  

</details>

#### Python & Pip

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

OR enable docker support to run python inside docker container

### Usage

Read [documentation](https://xvik.github.io/gradle-use-python-plugin/)

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [pom-plugin](https://github.com/xvik/gradle-pom-plugin) - improves pom generation
* [java-lib-plugin](https://github.com/xvik/gradle-java-lib-plugin) - avoid boilerplate for java or groovy library project
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
