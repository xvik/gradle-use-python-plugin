package ru.vyarus.gradle.plugin.python.cmd

/**
 * @author Vyacheslav Rusakov
 * @since 15.12.2017
 */
class VirtualenvExecTest extends AbstractCliMockSupport {

    Virtualenv env

    @Override
    void setup() {
        env= new Virtualenv(gradleEnv(), 'env')
    }

    def "Check execution"() {
        setup:
        mockExec(project, null, 0)

        when: "full create"
        env.create()
        then: "ok"
        logger.res =~ /\[python] python(3)? -m virtualenv env/

        when: "full create with copy"
        env.create(true)
        then: "ok"
        logger.res =~ /\[python] python(3)? -m virtualenv env --always-copy/

        when: "python only create"
        env.createPythonOnly()
        then: "ok"
        logger.res =~ /\[python] python(3)? -m virtualenv env --no-setuptools --no-pip --no-wheel/

        when: "python only create with copy"
        env.createPythonOnly(true)
        then: "ok"
        logger.res =~ /\[python] python(3)? -m virtualenv env --always-copy --no-setuptools --no-pip --no-wheel/

        when: "create without no pip"
        env.create(true, false, true)
        then: "ok"
        logger.res =~ /\[python] python(3)? -m virtualenv env --always-copy --no-pip --no-wheel/
    }
}
