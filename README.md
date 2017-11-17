# gradle-use-python-plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-use-python-plugin.svg)](https://travis-ci.org/xvik/gradle-use-python-plugin)

### About

Install pip dependencies and call python in gradle build

Features:
* Feature 1
* Feature 2

### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-use-python-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-use-python-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.use-python).


<!---
[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-use-python-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-use-python-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-use-python-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-use-python-plugin)
-->

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-use-python-plugin:0.1.0'
    }
}
apply plugin: 'ru.vyarus.use-python'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.use-python' version '0.1.0'
}
```

### Usage

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
