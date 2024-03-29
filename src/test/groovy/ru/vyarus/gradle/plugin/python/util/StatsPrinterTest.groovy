package ru.vyarus.gradle.plugin.python.util

import ru.vyarus.gradle.plugin.python.AbstractKitTest
import ru.vyarus.gradle.plugin.python.service.stat.PythonStat
import ru.vyarus.gradle.plugin.python.service.stat.StatsPrinter

/**
 * @author Vyacheslav Rusakov
 * @since 28.03.2024
 */
class StatsPrinterTest extends AbstractKitTest {

    def "Print simple stats"() {
        List<PythonStat> stats = []

        when: "print simple executions"
        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "gg",
                cmd: "something",
                start: 100,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "hh",
                cmd: "something-else",
                start: 120,
                success: true,
                duration: 10))

        String res = StatsPrinter.print(stats)
        println res

        then: "ok"
        unifyStats(res) == """
Python execution stats:

task                                        started         duration            
:gg                                         11:11:11:111    11ms                something
:hh                                         11:11:11:111    11ms                something-else

    Executed 2 commands in 11ms (overall)
"""

    }

    def "Print docker stats"() {
        List<PythonStat> stats = []

        when: "print simple executions"
        stats.add(new PythonStat(
                containerName: 'python12',
                projectPath: ":",
                taskName: "gg",
                cmd: "something",
                start: 100,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "hh",
                cmd: "something-else",
                start: 120,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                containerName: 'python13',
                projectPath: ":",
                taskName: "tt",
                cmd: "other",
                start: 130,
                success: true,
                duration: 10))

        String res = StatsPrinter.print(stats)
        println res

        then: "ok"
        unifyStats(res) == """
Python execution stats:

task                                        started        docker container     duration            
:gg                                         11:11:11:111   python12             11ms                something
:hh                                         11:11:11:111                        11ms                something-else
:tt                                         11:11:11:111   python13             11ms                other

    Executed 3 commands in 11ms (overall)
"""

    }

    def "Print failed stat"() {
        List<PythonStat> stats = []

        when: "print executions with failed command"
        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "gg",
                cmd: "something",
                start: 100,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "hh",
                cmd: "something-else",
                start: 120,
                success: false,
                duration: 10))

        String res = StatsPrinter.print(stats)
        println res

        then: "ok"
        unifyStats(res) == """
Python execution stats:

task                                        started         duration            
:gg                                         11:11:11:111    11ms                something
:hh                                         11:11:11:111    11ms                FAILED   something-else

    Executed 2 commands in 11ms (overall)
"""
    }

    def "Print parallel stats"() {
        List<PythonStat> stats = []

        when: "print executions with failed command"
        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "gg",
                cmd: "something",
                start: 100,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "hh",
                cmd: "something-else",
                start: 120,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "tt",
                cmd: "other",
                start: 125,
                success: true,
                duration: 10))

        String res = StatsPrinter.print(stats)
        println res

        then: "ok"
        unifyStats(res) == """
Python execution stats:

task                                        started         duration            
:gg                                         11:11:11:111    11ms                something
:hh                                      || 11:11:11:111    11ms                something-else
:tt                                      || 11:11:11:111    11ms                other

    Executed 3 commands in 11ms (overall)
"""
    }

    def "Print detected duplicates"() {
        List<PythonStat> stats = []

        when: "print executions with failed command"
        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "gg",
                cmd: "something",
                start: 100,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "hh",
                cmd: "something",
                start: 120,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "tt",
                cmd: "other",
                start: 130,
                success: true,
                duration: 10))

        stats.add(new PythonStat(
                projectPath: ":",
                taskName: "pp",
                cmd: "other",
                start: 140,
                success: true,
                duration: 10))

        String res = StatsPrinter.print(stats)
        println res

        then: "ok"
        unifyStats(res) == """
Python execution stats:

task                                        started         duration            
:gg                                         11:11:11:111    11ms                something
:hh                                         11:11:11:111    11ms                something
:tt                                         11:11:11:111    11ms                other
:pp                                         11:11:11:111    11ms                other

    Executed 4 commands in 11ms (overall)

    Duplicate executions:

\t\tsomething (2)
\t\t\t:gg   (work dir: null)
\t\t\t:hh   (work dir: null)

\t\tother (2)
\t\t\t:tt   (work dir: null)
\t\t\t:pp   (work dir: null)
"""
    }
}
