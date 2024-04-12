# Multi-module projects

When used in multi-module project, plugin will create virtualenv inside the root project directory
in order to share the same environment for all modules.

This could be changed with `python.envPath` configuration in modules.


## One environment for all modules

Project with 2 modules (+root):

```
/
    /mod1/
    /mod2/
    build.gradle
    settings.gradle
```

```groovy
plugins {
    id 'ru.vyarus.use-python' version '{{ gradle.version }}' apply false
}
                        
subprojects {
    apply plugin: 'ru.vyarus.use-python'                 
    
    python {
        pip 'click:6.7'
    }
}
```

Python plugin applied for submodules only (not for root project). One virtualenv will be created (at `/.gradle/python`) and used by both modules.

Note that plugins section in root project used for plugin version management.

## Root project use python too

If root project must use python tasks then use allprojects section instead:

```groovy
plugins {
    id 'ru.vyarus.use-python' version '{{ gradle.version }}' apply false
}
                        
allprojects {
    apply plugin: 'ru.vyarus.use-python'                 
    
    python {
        pip 'click:6.7'
    }
}
```

## Environment in module only

Suppose we want to use python only in one sub module (for example, for docs generation):

```
/
    /doc/
    /mod2/
    build.gradle
    settings.gradle
```

```groovy
plugins {
    id 'ru.vyarus.use-python' version '{{ gradle.version }}' apply false
}
    
// this may be inside module's build.gradle                    
project(':doc') {
    apply plugin: 'ru.vyarus.use-python'                 
    
    python {
        pip 'click:6.7'
    }
}
```

Python plugin applied only in docs module, but virtualenv will still be created at the root level.
If you want to move virtualenv itself inside module then specify relative path for it: `python.envPath = "python"`.

## Use different virtualenvs in modules

If modules require independent environments (different python versions required or incompatible modules used) then specify relative `envPath` so environment would be created relative to module dir.

```
/
    /mod1/
    /mod2/
    build.gradle
    settings.gradle
```

```groovy
plugins {
    id 'ru.vyarus.use-python' version '{{ gradle.version }}' apply false
}
                        
subprojects {
    apply plugin: 'ru.vyarus.use-python'                 
    
    python {
        envPath = 'python'
    }
}

// this may be inside module's build.gradle
project(':mod1') {
    python {
        pythonPath = "/path/to/python2"
        pip 'click:6.6'
    }
}

project(':mod2') {
    python {
        pythonPath = "/path/to/python3"
        pip 'click:6.7'
    }
}
```

Here `mod1` will cerate wirtualenv inside `/mod1/python` from python 2 and `mod2` will use its own environment created from python 3. 

## Problems resolution

Use python commands statistics report could help detect problems (enabled in root module):

```groovy
python.printStats = true 
```

[Report](stats.md#duplicates-detection) would show all executed commands and mark commands executed in parallel.