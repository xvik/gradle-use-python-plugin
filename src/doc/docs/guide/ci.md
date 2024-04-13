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
    name: Java {{ '${{ matrix.java }}' }}, python {{ '${{ matrix.python }}' }}
    strategy:
      matrix:
        java: [8, 11]
        python: ['3.8', '3.12']
      
      # reduce matrix, if required
      exclude:
          - java: 8
            python: '3.12'  

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK {{ '${{ matrix.java }}' }}
        uses: actions/setup-java@v1
        with:
          java-version: {{ '${{ matrix.java }}' }}

      - name: Set up Python {{ '${{ matrix.python }}' }}
        uses: actions/setup-python@v4
        with:
          python-version: {{ '${{matrix.python}}' }}

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
        - job_name: Java 8, python 3.8
          JAVA_HOME: C:\Program Files\Java\jdk1.8.0
          PYTHON: "C:\\Python38-x64"
        - job_name: Java 17, python 3.12
          JAVA_HOME: C:\Program Files\Java\jdk17
          appveyor_build_worker_image: Visual Studio 2019
          PYTHON: "C:\\Python312-x64"  

install:
  - set PATH=%PYTHON%;%PYTHON%\\Scripts;%PATH%
  - python --version
```         

Now plugin would be able to find python binary.

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

## Environment caching

To avoid creating virtual environments on each execution, it makes sense to move
environment location from the default `.gradle/python` (inside project) outside the project:

```groovy
python.envPath = '~/.myProjectEnv'
```

Virtual environment created inside the user directory and so could be easily cached now.

NOTE: Only `envPath` property supports home directory reference (`~/`). If you need it in other places
then use manual workaround: `'~/mypath/'.replace('~', System.getProperty("user.home"))`

## System packages

On linux distributions, some python packages could be managed with external packages
(like python3-venv, python3-virtualenv, etc.).

If your build is **not using virtual environment** and still needs to install such packages,
it would lead to error:

```
error: externally-managed-environment

× This environment is externally managed
╰─> To install Python packages system-wide, try apt install
    python3-xyz, where xyz is the package you are trying to
    install.
```

To work around this problem, use [breakSystemPackages](https://pip.pypa.io/en/stable/cli/pip_install/#cmdoption-break-system-packages) option:


```groovy
python {
    breakSystemPackages = true
}
```
