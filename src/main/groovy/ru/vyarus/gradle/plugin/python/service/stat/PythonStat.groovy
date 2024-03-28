package ru.vyarus.gradle.plugin.python.service.stat

import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull

/**
 * Python command execution statistic. Also tracks internal direct commands execution (just simpler to count all),
 *
 * @author Vyacheslav Rusakov
 * @since 22.03.2024
 */
@CompileStatic
class PythonStat implements Comparable<PythonStat> {
    // docker
    String containerName
    String projectPath
    String taskName
    String cmd
    long start
    long duration
    boolean success

    boolean parallel

    @Override
    int compareTo(@NotNull PythonStat pythonStat) {
        return start <=> pythonStat.start
    }

    boolean inParallel(PythonStat stat) {
        return startIn(this, stat) || startIn(stat, this)
    }

    String getFullTaskName() {
        return "$projectPath:$taskName".replaceAll('::', ':')
    }

    @Override
    String toString() {
        return "$fullTaskName:${System.identityHashCode(this)}"
    }

    private static boolean startIn(PythonStat stat, PythonStat stat2) {
        return stat.start <= stat2.start && stat.start + stat.duration > stat2.start
    }
}
