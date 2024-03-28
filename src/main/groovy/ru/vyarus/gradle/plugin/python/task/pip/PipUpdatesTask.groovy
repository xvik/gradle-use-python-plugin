package ru.vyarus.gradle.plugin.python.task.pip

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Print available new versions for the registered pip modules.
 *
 * @author Vyacheslav Rusakov
 * @since 01.12.2017
 */
@CompileStatic
abstract class PipUpdatesTask extends BasePipTask {

    /**
     * True to show all available updates. By default (false): show only updates for configured modules.
     */
    @Input
    boolean all

    @TaskAction
    @SuppressWarnings('DuplicateNumberLiteral')
    void run() {
        boolean showAll = getAll()
        if (!showAll && modulesList.empty) {
            logger.lifecycle('No modules declared')
        } else {
            List<String> res = []
            List<String> updates = pip.readOutput('list -o -l --format=columns').toLowerCase().readLines()

            // when no updates - no output (for all or filtered)
            if (showAll || updates.empty) {
                res = updates
            } else {
                // header
                res.addAll(updates[0..1])
                2.times { updates.remove(0) }

                // search for lines matching modules
                modulesList.each { PipModule mod ->
                    String line = updates.find { it =~ /$mod.name\s+/ }
                    if (line) {
                        res.add(line)
                    }
                }
            }

            if (res.size() > 2) {
                logger.lifecycle('The following modules could be updated:\n\n{}',
                        res.collect { '\t' + it }.join('\n'))
            } else {
                logger.lifecycle('All modules use the most recent versions')
            }
        }
    }
}
