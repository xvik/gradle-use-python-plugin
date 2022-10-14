# Call python

Call python command:

```groovy
task cmd(type: PythonTask) {
    command = "-c print('sample')"
}
```

called: `python -c print('sample')` on win and `python -c exec("print('sample')")` on *nix (exec applied automatically for compatibility)

Call multi-line command:

```groovy
task cmd(type: PythonTask) {
    command = "-c \"import sys; print(sys.prefix)\""
}
```

called: `python -c "import sys; print(sys.prefix)"` on win and `python -c exec("import sys; print(sys.prefix)")` on *nix

!!! note 
    It is important to wrap script with space in quotes (otherwise parser will incorrectly parse arguments).

## Pip module command

```groovy
task mod(type: PythonTask) {
    module = 'sample' 
    command = "mod args"
}
```

called: `python -m sample mod args`

## Script

```groovy
task script(type: PythonTask) { 
    command = "path/to/script.py 1 2"
}
```

called: `python path/to/script.py 1 2` (arguments are optional, just for demo)

String command is used for simplicity, but it could be array/collection of args:

```groovy
task script(type: PythonTask) { 
    command = ['path/to/script.py', '1', '2'] 
}
```

## Command parsing

When command passed as string it is manually parsed to arguments array (split by space):

* Spaces in quotes are ignored: `"quoted space"` or `'quoted space'`
* Escaped spaces are ignored: `with\\ space` (argument will be used with simple space then - escape removed).
* Escaped quotes are ignored: `"with \\"interrnal quotes\\" inside"`. But pay attention that it must be 2 symbols `\\"` and **not** `\"` because otherwise it is impossible to detect escape.

To view parsed arguments run gradle with `-i` flag (enable info logs). In case when command can't be parsed properly
(bug in parser or unsupported case) use array of arguments instead of string.

## Environment variables

By default, executed python can access system environment variables (same as `System.getenv()`).

To declare custom (process specific) variables:

```groovy
task sample(type: PythonTask) {
       command = "-c \"import os;print('variables: '+os.getenv('some', 'null')+' '+os.getenv('foo', 'null'))\""
       environment 'some', 1
       environment 'other', 2
       environment(['foo': 'bar', 'baz': 'bag'])
}
```

Map based declaration (`environment(['foo': 'bar', 'baz': 'bag'])`) does not remove previously declared variables
(just add all vars from map), but direct assignment `environment = ['foo': 'bar', 'baz': 'bag']` will reset variables.

System variables will be available even after declaring custom variables (of course, custom variables could override global value).

!!! note 
    Environment variable could also be declared in extension to apply for all python commands:
    `python.environment 'some', 1` (if environments declared both globally (through extension) and directly on task, they would be merged)
