package ru.vyarus.gradle.plugin.python.task.pip

import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction

/**
 * List all installed modules in current scope. The same is displayed after pipInstall by default.
 * Task used just to be able to see installed modules list at any time (because pipInstall will show it only once).
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2017
 */
@CompileStatic
class PipListTask extends BasePipTask {

    @TaskAction
    void run() {
        pip.exec('list --format=columns')
    }
}
