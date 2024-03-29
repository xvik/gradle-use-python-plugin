package ru.vyarus.gradle.plugin.python.util

import ru.vyarus.gradle.plugin.python.service.stat.PythonStat
import ru.vyarus.gradle.plugin.python.service.stat.StatsPrinter
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 28.03.2024
 */
class StatsPrinterTest extends Specification {

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
        res == """
Python execution stats:

task                                        started         duration            
:gg                                         07:00:00:100    10ms                something
:hh                                         07:00:00:120    10ms                something-else

    Executed 2 commands in 20ms (overall)
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
        res == """
Python execution stats:

task                                        started        docker container     duration            
:gg                                         07:00:00:100   python12             10ms                something
:hh                                         07:00:00:120                        10ms                something-else
:tt                                         07:00:00:130   python13             10ms                other

    Executed 3 commands in 30ms (overall)
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
        res == """
Python execution stats:

task                                        started         duration            
:gg                                         07:00:00:100    10ms                something
:hh                                         07:00:00:120    10ms       FAILED   something-else

    Executed 2 commands in 20ms (overall)
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
        res == """
Python execution stats:

task                                        started         duration            
:gg                                         07:00:00:100    10ms                something
:hh                                      || 07:00:00:120    10ms                something-else
:tt                                      || 07:00:00:125    10ms                other

    Executed 3 commands in 30ms (overall)
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
        res == """
Python execution stats:

task                                        started         duration            
:gg                                         07:00:00:100    10ms                something
:hh                                         07:00:00:120    10ms                something
:tt                                         07:00:00:130    10ms                other
:pp                                         07:00:00:140    10ms                other

    Executed 4 commands in 40ms (overall)

    Duplicate executions:

\t\tsomething (2)
\t\t\t:gg
\t\t\t:hh

\t\tother (2)
\t\t\t:tt
\t\t\t:pp
"""
    }
}
