package ru.vyarus.gradle.plugin.python.cmd

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipExecTest extends AbstractCliMockSupport {

    Pip pip

    @Override
    void setup() {
        // required for virtualenv detection in pip (which must not detect env now)
        // (this can't be done with spies or mocks)
        execCase({it.contains('sys.prefix')}, '3.5\n/usr/\n/usr/python3')
        pip = new Pip(gradleEnv())
    }

    def "Check execution"() {
        setup:
        mockExec(project, 'sample output', 0)

        when: "call install module"
        pip.install('mod')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip install mod --user\n\t sample output\n/

        when: "call pip cmd"
        logger.reset()
        pip.exec('list --format')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip list --format --user\n\t sample output\n/

        when: "call freeze"
        logger.reset()
        pip.exec('freeze')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip freeze --user\n\t sample output\n/
    }

    def "Check global scope usage"() {
        setup:
        mockExec(project, null, 0)

        when: "call in user scope"
        pip.install('mod')
        then: "ok"
        logger.res =~ /\[python] python(3)? -m pip install mod --user/

        when: "call in global scope"
        logger.reset()
        pip.inGlobalScope { pip.install('mod') }
        then: "ok"
        !(logger.res =~ /\[python] python(3)? -m pip install mod --user/)

        when: "call in user scope"
        logger.reset()
        pip.install('mod')
        then: "scope is correct"
        logger.res =~ /\[python] python(3)? -m pip install mod --user/
    }

    def "Check pip cache disable for installation"() {
        setup:
        mockExec(project, null, 0)
        pip.useCache = false

        when: "call install without cache"
        pip.install('mod')
        then: "flag applied"
        logger.res =~ /\[python] python(3)? -m pip install mod --user --no-cache-dir/

        when: "call different command"
        pip.exec('list')
        then: "no flag applied"
        logger.res =~ /\[python] python(3)? -m pip list --user/

        cleanup:
        pip.useCache = true
    }

    def "Check pip extraIndexUrls for installation"() {
        setup:
        mockExec(project, null, 0)
        pip.extraIndexUrls = ["http://extra-url.com", "http://another-url.com"]

        when: "call install with extra index urls"
        pip.install('mod')
        then: "flag applied"
        logger.res =~ /\[python] python(3)? -m pip install mod --user --extra-index-url http:\/\/extra-url\.com --extra-index-url http:\/\/another-url\.com/

        when: "call list with extra index urls"
        pip.exec('list')
        then: "flag applied"
        logger.res =~ /\[python] python(3)? -m pip list --user --extra-index-url http:\/\/extra-url\.com --extra-index-url http:\/\/another-url\.com/

        when: "call freeze command"
        pip.exec('freeze')
        then: "no flag applied"
        logger.res =~ /\[python] python(3)? -m pip freeze --user/

        cleanup:
        pip.extraIndexUrls = []
    }

    def "Check pip extraIndexUrls with credentials"() {
        setup:
        mockExec(project, null, 0)
        pip.extraIndexUrls = ["http://user:pass@extra-url.com"]

        when: "call install with extra index urls"
        pip.install('mod')
        then: "flag applied"
        logger.res =~ /\[python] python(3)? -m pip install mod --user --extra-index-url http:\/\/user:\*{5}@extra-url\.com/

        cleanup:
        pip.extraIndexUrls = []
    }

    def "Check pip extraIndexUrls with multiple credentials"() {
        setup:
        mockExec(project, null, 0)
        pip.extraIndexUrls = ["http://user:pass@extra-url.com", "https://user22:pass22@another-url.com"]

        when: "call list with extra index urls"
        pip.exec('list')
        then: "flag applied"
        logger.res =~ /\[python] python(3)? -m pip list --user --extra-index-url http:\/\/user:\*{5}@extra-url\.com --extra-index-url https:\/\/user22:\*{5}@another-url\.com/

        cleanup:
        pip.extraIndexUrls = []
    }

    def "Check pip trustedHosts for installation"() {
        setup:
        mockExec(project, null, 0)
        pip.trustedHosts = ["extra-url.com", "another-url.com"]

        when: "call install with extra index urls"
        pip.install('mod')
        then: "flag applied"
        logger.res =~ /\[python] python(3)? -m pip install mod --user --trusted-host extra-url\.com --trusted-host another-url\.com/

        when: "call different command"
        pip.exec('list')
        then: "no flag applied"
        logger.res =~ /\[python] python(3)? -m pip list --user/

        cleanup:
        pip.trustedHosts = []
    }

    def "Check pip break system packages"() {
        setup:
        mockExec(project, null, 0)
        pip.breakSystemPackages(true)

        when: "new option applied"
        pip.install('mod')
        then: "flag applied"
        logger.res =~ /\[python] python(3)? -m pip install mod --user --break-system-packages/

        when: "call different command"
        pip.exec('list')
        then: "no flag applied"
        logger.res =~ /\[python] python(3)? -m pip list --user/

        cleanup:
        pip.breakSystemPackages(false)
    }

}
