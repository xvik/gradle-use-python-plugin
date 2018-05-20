package ru.vyarus.gradle.plugin.python.task

import ru.vyarus.gradle.plugin.python.task.pip.module.ModuleFactory
import ru.vyarus.gradle.plugin.python.task.pip.PipModule
import ru.vyarus.gradle.plugin.python.task.pip.module.VcsPipModule
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
        mod.toPipInstallString() == 'click==6.7'

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

    def "Check vcs module parse"() {

        when:
        VcsPipModule res = PipModule.parse("git+https://git.example.com/MyProject@v1.0#egg=MyProject-6.6")
        then:
        res.declaration == "git+https://git.example.com/MyProject@v1.0#egg=MyProject"
        res.name == 'MyProject'
        res.version == '6.6'
        res.toString() == "MyProject 6.6 (git+https://git.example.com/MyProject@v1.0#egg=MyProject)"
        res.toPipString() == 'MyProject==6.6'
        res.toPipInstallString() == res.declaration

        when:
        res = PipModule.parse("git+https://git.example.com/MyProject@v1.0#egg=MyProject-6.6&subdirectory=pkg_dir")
        then:
        res.declaration == "git+https://git.example.com/MyProject@v1.0#egg=MyProject&subdirectory=pkg_dir"
        res.name == 'MyProject'
        res.version == '6.6'
        res.toString() == "MyProject 6.6 (git+https://git.example.com/MyProject@v1.0#egg=MyProject&subdirectory=pkg_dir)"
        res.toPipString() == "MyProject==6.6"
        res.toPipInstallString() == res.declaration
    }

    def "Check old api supported"() {

        when:
        VcsPipModule res = PipModule.parse("git+https://git.example.com/MyProject@v1.0#egg=MyProject-6.6")
        then:
        res.declaration == "git+https://git.example.com/MyProject@v1.0#egg=MyProject"
        res.name == 'MyProject'
        res.version == '6.6'
        res.toString() == "MyProject 6.6 (git+https://git.example.com/MyProject@v1.0#egg=MyProject)"
        res.toPipString() == 'MyProject==6.6'
        res.toPipInstallString() == res.declaration
    }

    def "Check vcs module errors"() {

        when: "no @version part"
        PipModule.parse("git+https://git.example.com/MyProject#egg=MyProject-6.6")
        then: "err"
        def ex = thrown(IllegalArgumentException)
        ex.message == "Incorrect pip vsc module declaration: 'git+https://git.example.com/MyProject#egg=MyProject-6.6' " +
                "(required format is 'vcs+protocol://repo_url/@vcsVersion#egg=name-pkgVersion'). '@version' part is required"

        when: "no module name"
        PipModule.parse("git+https://git.example.com/MyProject@4f32s432ff4233#eg=MyProject-6.6")
        then: "err"
        ex = thrown(IllegalArgumentException)
        ex.message == "Incorrect pip vsc module declaration: 'git+https://git.example.com/MyProject@4f32s432ff4233#eg=MyProject-6.6' " +
                "(required format is 'vcs+protocol://repo_url/@vcsVersion#egg=name-pkgVersion'). Module name not found"

        when: "no module version"
        PipModule.parse("git+https://git.example.com/MyProject@4f32s432ff4233#egg=MyProject")
        then: "err"
        ex = thrown(IllegalArgumentException)
        ex.message == "Incorrect pip vsc module declaration: 'git+https://git.example.com/MyProject@4f32s432ff4233#egg=MyProject' " +
                "(required format is 'vcs+protocol://repo_url/@vcsVersion#egg=name-pkgVersion'). Module version is required in module " +
                "(#egg=name-version): 'MyProject'. This is important to be able to check up-to-date state without python run"

        when: "bad module name"
        PipModule.parse("git+https://git.example.com/MyProject@4f32s432ff4233#egg=My-Project-6.6")
        then: "err"
        ex = thrown(IllegalArgumentException)
        ex.message == "Incorrect pip vsc module declaration: 'git+https://git.example.com/MyProject@4f32s432ff4233#egg=My-Project-6.6' " +
                "(required format is 'vcs+protocol://repo_url/@vcsVersion#egg=name-pkgVersion'). module name (#egg= part) contains " +
                "multiple '-' symbols: 'My-Project-6.6'"
    }

    def "Check vsc module equals"() {

        when: "two equal modules"
        PipModule mod = new PipModule('one', '1')
        PipModule mod2 = new VcsPipModule('git+https://git.example.com/one@v1.0#egg=one-1', 'one', '1')
        then: "equal"
        mod.hashCode() == mod2.hashCode()
        mod.equals(mod2)
        mod.equals(mod)
        !mod.equals(new Object())
    }

    def "Check module search"() {

        def mods = [
                'git+https://git.example.com/something@v1.0#egg=something-6.6',
                'some:1.2',
                'git+https://git.example.com/MyProject@v1.0#egg=MyProject-6.6',
        ]

        expect:
        ModuleFactory.findModuleDeclaration('sOme', mods) == 'some:1.2'
        ModuleFactory.findModuleDeclaration('myproject', mods) == 'git+https://git.example.com/MyProject@v1.0#egg=MyProject-6.6'
        ModuleFactory.findModuleDeclaration('something', mods) == 'git+https://git.example.com/something@v1.0#egg=something-6.6'
    }
}