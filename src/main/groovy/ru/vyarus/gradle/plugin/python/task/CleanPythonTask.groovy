package ru.vyarus.gradle.plugin.python.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.python.PythonExtension

/**
 * Cleanup local python environment. Required to re-create environment or when python version must be changed.
 * In case of docker, remove is performed inside docker because environment is created with a root user and
 * would not be deleted directly.
 *
 * @author Vyacheslav Rusakov
 * @since 11.10.2022
 */
@CompileStatic
class CleanPythonTask extends BasePythonTask {

    /**
     * Environment location (by default {@link PythonExtension#envPath}).
     */
    @Input
    String envPath

    @TaskAction
    void run() {
        String path = project.file(getEnvPath()).absolutePath
        if (dockerUsed) {
            // with docker, environment would be created with a root user and so it would not be possible
            // to simply remove folder: so removing within docker
            String[] cmd = windows ? ['rd', '/s', '/q', "\"$path\""] : ['rm', '-rf', path]
            if (dockerExec(cmd) != 0) {
                throw new GradleException('Python environment cleanup failed')
            }
        } else {
            project.delete(path)
        }
    }
}
