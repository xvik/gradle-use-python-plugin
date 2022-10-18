# Docker

Instead of direct python usage, plugin could use python docker container
(docker must be installed).

To enable docker support:

```groovy
python.docker.use = true
```

!!! important
    Docker support is implemented with [testcontainers](https://www.testcontainers.org/) because
    testcontainers will always correctly cleanup used containers (extremely important for build failures or "infinite" runs). 
    But testcontainers currently only **work on linux containers** and so you [will not be able to use 
    windows python containers](https://www.testcontainers.org/supported_docker_environment/windows/). 
    It does not mean it can't be used on windows: it **will run on WSL2 (default mode) or Hyper-V** (running linux containers on windows).

!!! warning
    Testcontainers [will not work on windows server](https://github.com/testcontainers/testcontainers-java/issues/2960) and so 
    docker support will not work on most **windows CI** servers (like appveyor). 
    Linux CI is completely ok (e.g. works out of the box on github actions)

## Image

By default, [official python docker image](https://hub.docker.com/_/python) used: `python:3.10.7-alpine3.15`.
But you can change it to any other image containing python:

```groovy
python.docker.image = 'python:3.10.7-alpine3.15'
```

!!! tip
    It is highly recommended to always specify exact tags for reproducible builds!

Simple image declaration above would lead to docker hub, but if you need to use 
custom repository simply declare it: `registry.mycompany.com/mirror/python:3.10.7-alpine3.15`

## Behaviour

* Docker container is started before first python call (in `checkPython` task) and stopped after build completion.
* Entire project directory is mapped inside container (so python process would be able to access any project file)
* Working directory would be set to project root

It all mentioned in logs:

```
> Task :checkPython
[docker] container 'python:3.10.7-alpine3.15' (/focused_wing) started in 1.92s
	Mount           /home/user/projects/project:/usr/source/project
	Work dir        /usr/source/project
```

!!! note
    Docker container kept started to speed-up execution (avoid re-starting it for each command),
    but this also cause **python command logs to appear only after execution** (current api limitation). This might be a bit confusing
    for non-immediate tasks like `pipInstall`, but it's a compromise.

Docker containers are **stateless** (removed after execution) and so, if pip modules used,
prefer virtualenv mode (default) because in this case virtualenv would be created inside 
project "caching" all required pip packages. so workflow would be:

* Docker container started
* Virtualenv installed into it
* Virtualenv folder created (inside project)
* Pip install executed for created python copy

On next execution, created environment would be simply used (same as with direct python usage).

!!! note
    Gradle itself is executed on host and only python commands are executed inside container.

## Configuration

`python.docker` properties:

name | Description                                                                                                           | Default
------|-----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------
use | Enable docker support                                                                                                 | false
image | Image name (could be full path with repository)                                                                       | python:3.10.7-alpine3.15
windows | Windows container OS. Windows containers support implemented in plugin, but currently not supported by testcontainers | false
ports | Ports to expose from container (for long-lived commands) | 

!!! note
    Docker support is experimental and, current configuration is a minimal working configuration.
    If you have a case requiring more configuration options, please [create new issue](https://github.com/xvik/gradle-use-python-plugin/issues)

Docker configuration could be changed globally or per-task. For example:

```groovy
task sample(type: PythonTask) {
        workDir 'src/main'
        docker.ports 5000
        environment 'foo', 'bar'
        command = '-c print(\'samplee\')'
    }
```

This changes `workDir`, docker exposed ports and environment variables.

Plugin will detect these changed and **will restart container** automatically to properly apply configuration (you'll see it in log).

!!! tip
    To avoid redundant container restarts prefer global docker configuration. But `workDir` could be set
    only on task level

Task properties affecting container restart (because they could be specified only during startup):

name | Description                                                                                                           
-----|-----------------------------------------------------------------------------------------------------------------------
docker.ports | container restarts if ports configuration differ (not all required ports exposed)
environment | container restarts if varaibles change (if more variables used for container start - it's ok)
workDir | container restarts if work dir changes


!!! note
    Task also has `docker.use` option and so docker could be enabled/disabled for exact task, but it will fail if pip modules required   

!!! tip
    All paths used in python commands are processed to represent correct in-container paths.
    This should simplify usage as no changes required to use task with direct python and inside docker.
    Also, all path separators changed according to target os (important when running container from windows host).

### Exclusive mode

There is one configuration option available **only for tasks** - exclusive mode:

```groovy
task sample(type: PythonTask) {
        docker.exclusive = true
        module = 'mkdocs'
        command = 'serve'
    }
```

In this case new docker container would be started **using python command as container command** (command keeping container alive).
This mode required for long-lived or just long tasks because this way logs immediately streamed into
gradle console. This is ideal for various dev-servers (like mkdocs dev server in example above).

Exclusive task does not stop currently running (shared) container (because other (potential) python tasks may need it).

!!! important
    When running infinite task (e.g. dev server) the only way to stop it is using force (usually stop from IDE).
    After such emergency stop containers may live for about ~10s (keep it in mind), but, eventually, they should be
    removed.

### Ports

Exposing container ports for host might be useful for long-lived tasks (usually, `exclusive` tasks, but not necessary).

There is a special method for ports configuration in both global extension and tasks:

```groovy
python.docker.ports 5000, 5010
```

Ports might be declared as integer and string. In this simple case, ports would be mapped on **the same** host ports.

In order to map port on different host port use colon-syntax:

```groovy
python.docker.ports '5000:5001', '5010:5011'
```

Here docker port 5000 mapped to host 5001 and 5010 to 5011

### Different image

Custom task can use a different container image (then declared in global extension) if required (no limits):

```groovy
task sample(type: PythonTask) {
        docker.image = 'python:3.10.7-bullseye'
        module = 'mkdocs'
        command = 'serve'
    }
```

In this case, new container would be started just before python command (and will stay up until gradle finished build).

## Concurrency

Any number of docker images could be used during the build. Different images would work concurrently.
Different tags for the same image would be treated as different containers.

Execution is synchronized by docker image: in multi-module project or with parallel build only
one python command would be executed in container at a time (but commands could run in different containers concurrently).

This synchronization applied to minimize potential edge cases (simpler to investigate problems, avoid side effects).
Later this could change (based on feedback).

