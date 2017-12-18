package ru.vyarus.gradle.plugin.python.task

import ru.vyarus.gradle.plugin.python.task.pip.PipModule
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

    def "Check module hash equals"() {

        when: "two equal modules"
        PipModule mod = new PipModule('one', '1')
        PipModule mod2 = new PipModule('one', '1')
        then: "equal"
        mod.hashCode() == mod2.hashCode()
        mod.equals(mod2)
    }

    def "Check module creation error"() {

        when: "module without version"
        new PipModule('one', null)
        then: "err"
        thrown(AssertionError)

        when: "module without name"
        new PipModule(null, '1')
        then: "err"
        thrown(AssertionError)
    }
}