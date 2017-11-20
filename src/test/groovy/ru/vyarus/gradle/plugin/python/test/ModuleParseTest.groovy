package ru.vyarus.gradle.plugin.python.test

import ru.vyarus.gradle.plugin.python.task.PipModule
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class ModuleParseTest extends Specification {

    def "Check module declaration"() {

        when: "parse declaration"
        PipModule mod = PipModule.parse('click:6.7')
        then: "parsed"
        mod.toString() == 'click 6.7'
        mod.toPipString() == 'click==6.7'

        when: "error declaration"
        PipModule.parse(null)
        then: "err"
        thrown(NullPointerException)

        when: "error declaration"
        PipModule.parse('click')
        then: "err"
        thrown(IllegalArgumentException)
    }
}