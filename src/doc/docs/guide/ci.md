# CI

Example configuration, required to use python on CI servers.

!!! warning
    [Docker support](docker.md) will not work on most **windows CI** servers (like appveyor).
    Linux CI is completely ok (e.g. works out of the box on github actions)

## GitHub actions

```yaml
name: CI

on:
  push:
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    name: Java ${{ matrix.java }}, python ${{ matrix.python }}
    strategy:
      matrix:
        java: [8, 11]

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v1
        with:
          java-version: ${{ matrix.java }}

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: 3.10

      - name: Build
        run: |
          chmod +x gradlew
          python --version
          pip --version
          ./gradlew assemble --no-daemon

      - name: Test
        run: ./gradlew check --no-daemon
```

## Appveyour

To make plugin work on [appveyour](https://www.appveyor.com/) you'll need to add python to path:

```yaml
environment:
    matrix:
        - JAVA_HOME: C:\Program Files\Java\jdk1.8.0
          PYTHON: "C:\\Python36-x64"

install:
  - set PATH=%PYTHON%;%PYTHON%\\Scripts;%PATH%
  - python --version
```         

Now plugin would be able to find python binary.

To use python 3.9 you'll need to switch image:

```yaml
image: Visual Studio 2019
```

See [available pythons matrix](https://www.appveyor.com/docs/windows-images-software/#python) for more info.

## Travis

To make plugin work on [travis](https://travis-ci.org/) you'll need to install python3 packages:

```yaml
language: java  
dist: bionic
jdk: openjdk8

addons:
  apt:
    packages:
    - python3
    - python3-pip
    - python3-setuptools 

before_install:
  - python3 --version
  - pip3 --version
  - pip3 install -U pip
``` 

It will be python 3.6 by default (for bionic).
