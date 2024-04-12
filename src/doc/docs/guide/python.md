# Python & Pip

!!! tip
    [Docker might be used](docker.md) instead of direct python installation

To make sure python and pip are installed:

```bash
python --version  
pip --version
```

On *nix `python` usually reference python2. For python3:

```bash
python3 --version  
pip3 --version
```

!!! tip
    [Python-related configurations](configuration.md#python-location)

## Windows install

[Download and install](https://www.python.org/downloads/windows/) python manually or use
[chocolately](https://chocolatey.org/packages/python/3.6.3):

```bash
choco install python
```

In Windows 10 python 3.9 could be installed from Windows Store:
just type 'python' in console and windows will open Windows Store's python page.
No additional actions required after installation.

Note that windows store python will require minium virtualenv 20.0.11 (or above).
(if virtualenv not yet installed then no worry - plugin will install the correct version)

## Linux/Macos install

On most *nix distributions python is already installed, but often without pip.

[Install](https://pip.pypa.io/en/stable/installing/) pip if required (ubuntu example):

```bash
sudo apt-get install python3-pip
```

Make sure the latest pip installed (required to overcome some older pip problems):

```bash
pip3 install -U pip
```

To install exact pip version:

```bash
pip3 install -U pip==20.0.11
```

Note that on ubuntu pip installed with `python3-pip` package is 9.0.1, but it did not(!) downgrade
module versions (e.g. `pip install click 6.6` when click 6.7 is installed will do nothing).
Maybe there are other differences, so it's highly recommended to upgrade pip with `pip3 install -U pip`.

If you need to switch python versions often, you can use [pyenv](https://github.com/pyenv/pyenv):
see [this article](https://www.liquidweb.com/kb/how-to-install-pyenv-on-ubuntu-18-04/) for ubuntu installation guide.
But pay attention to PATH: plugin may not "see" pyenv due to [different PATH](configuration.md#python-location) (when not launched from shell).

### Externally managed environment

On linux, multiple python packages could be installed. For example:

```
sudo apt install python3.12
```

Install python 3.12 accessible with `python3.12` binary, whereas `python3` would be a different python (e.g. 3.9)

To use such python specify:

```groovy
python {
    pythonBinary = 'python3.12'
    breakSystemPackages = true
}
```

`breakSystemPackages` is required if you need to install pip modules and target python
does not have virtualenv installed (so plugin would try to install it).

Without `breakSystemPackages` you'll see the following error:

```
error: externally-managed-environment

× This environment is externally managed
╰─> To install Python packages system-wide, try apt install
    python3-xyz, where xyz is the package you are trying to
    install.
```

### Possible pip issue warning (linux/macos)

If `pip3 list -o` fails with: `TypeError: '>' not supported between instances of 'Version' and 'Version'`
Then simply update installed pip version: `python3 -m pip install --upgrade pip`

This is a [known issue](https://github.com/pypa/pip/issues/3057) related to incorrectly
patched pip packages in some distributions.

## Automatic pip upgrade

As described above, there are different ways of pip installation in linux and, more important,
admin permissions are required to upgrade global pip. So it is impossible to upgrade pip from the plugin (in all cases).

But, it is possible inside virtualenv or user (--user) scope. Note that plugin creates virtualenv by default (per project independent python environment).

So, in order to use newer pip simply put it as first dependency:

```
python {
    pip 'pip:10.0.1'
    pip 'some_module:1.0'
}
```

Here project virtualenv will be created with global pip and newer pip version installed inside environment.
Packages installation is sequential, so all other packages will be installed with newer pip (each installation is independent pip command).

The same will work for user scope: `python.scope = USER`

When applying this trick, consider minimal pip version declared in configuration
(`python.minPipVersion='9'` by default) as minimal pip version required for *project setup*
(instead of minimal version required *for work*).

## Automatic python install

Python is assumed to be used as java: install and forget. It perfectly fits user
use case: install python once and plugin will replace all manual work on project environment setup.

It is also easy to configure python on CI (like travis).

If you want automatic python installation, try looking on JetBrain's
[python-envs plugin](https://github.com/JetBrains/gradle-python-envs). But be careful because
it has some caveats (for example, on windows python could be installed automatically just once
and requires manual un-installation). 

## Global python validation

For global python (when no `pythonPath` configured) plugin would manually search
for python binary in `$PATH` and would throw error if not found containing 
entire `$PATH`. This is required for cases when PATH visible for gradle process
is different to your shell path.

For example, on M1 it could be rosetta path instead of native (see [this issue](https://github.com/xvik/gradle-use-python-plugin/issues/35)).

Validation could be disabled with:

```groovy
python.validateSystemBinary = false
```

!!! note
    This option is ignored if [docker support](docker.md) enabled