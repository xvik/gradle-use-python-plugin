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
        mod.equals(mod)
        !mod.equals(new Object())
    }

    def "Check module creation error"() {

        when: "module without version"
        PipModule.parse('one: ')
        then: "err"
        thrown(IllegalArgumentException)

        when: "module without name"
        PipModule.parse(' :1')
        then: "err"
        thrown(IllegalArgumentException)
    }
}