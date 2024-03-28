package ru.vyarus.gradle.plugin.python.service.stat

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.python.util.DurationFormatter

import java.text.SimpleDateFormat

/**
 * Python execution statistics print utility.
 *
 * @author Vyacheslav Rusakov
 * @since 28.03.2024
 */
@CompileStatic
class StatsPrinter {

    @SuppressWarnings(['SimpleDateFormatMissingLocale', 'Println'])
    static String print(List<PythonStat> stats) {
        if (stats.empty) {
            return ''
        }
        Set<PythonStat> sorted = new TreeSet<PythonStat>(stats)
        StringBuilder res = new StringBuilder('\nPython execution stats:\n\n')
        boolean dockerUsed = stats.stream().anyMatch { it.containerName != null }
        SimpleDateFormat timeFormat = new SimpleDateFormat('HH:mm:ss:SSS')
        StatCollector collector = new StatCollector(sorted)
        String format = dockerUsed ? '%-37s    %-3s%-12s   %-20s %-10s %-8s %s%n'
                : '%-37s    %-3s%-12s   %s %-10s %-8s %s%n'
        res.append(String.format(
                format, 'task', '', 'started', dockerUsed ? 'docker container' : '', 'duration', '', ''))

        for (PythonStat stat : (sorted)) {
            collector.collect()
            res.append(String.format(format, stat.fullTaskName, stat.parallel ? '||' : '',
                    timeFormat.format(stat.start),
                    stat.containerName ?: '', DurationFormatter.format(stat.duration),
                    stat.success ? '' : 'FAILED', stat.cmd))
        }
        res.append('\n    Executed ').append(stats.size()).append(' commands in ')
                .append(DurationFormatter.format(collector.overall)).append(' (overall)\n')

        if (!collector.duplicates.isEmpty()) {
            res.append('\n    Duplicate executions:\n')
            collector.duplicates.each {
                res.append("\n\t\t$it.key (${it.value.size()})\n")
                it.value.each {
                    res.append("\t\t\t$it.fullTaskName\n")
                }
            }
        }
        return res.toString()
    }

    @SuppressWarnings('NestedForLoop')
    static class StatCollector {
        long overall = 0

        Map<String, List<PythonStat>> duplicates = [:]

        StatCollector(Set<PythonStat> stats) {
            for (PythonStat stat : stats) {
                for (PythonStat stat2 : stats) {
                    if (stat != stat2 && stat.inParallel(stat2)) {
                        stat.parallel = true
                        stat2.parallel = true
                    }
                }

                List<PythonStat> dups = duplicates.get(stat.cmd)
                if (dups == null) {
                    dups = []
                    duplicates.put(stat.cmd, dups)
                }
                dups.add(stat)

                overall += stat.duration
            }
            duplicates.removeAll {
                it.value.size() == 1
            }
        }
    }
}
