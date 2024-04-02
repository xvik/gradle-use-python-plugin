package ru.vyarus.gradle.plugin.python.cmd

/**
 * @author Vyacheslav Rusakov
 * @since 02.04.2024
 */
class VenvExecTest extends AbstractCliMockSupport {

    Venv env

    @Override
    void setup() {
        env= new Venv(gradleEnv(), 'env')
    }

    def "Check execution"() {
        setup:
        mockExec(project, null, 0)

        when: "full create"
        env.create()
        then: "ok"
        logger.res =~ /\[python] python(3)? -m venv env/

        when: "full create with copy"
        env.create(true)
        then: "ok"
        logger.res =~ /\[python] python(3)? -m venv env --copies/

        when: "python only create"
        env.createPythonOnly()
        then: "ok"
        logger.res =~ /\[python] python(3)? -m venv env --without-pip/

        when: "python only create with copy"
        env.createPythonOnly(true)
        then: "ok"
        logger.res =~ /\[python] python(3)? -m venv env --copies --without-pip/
    }
}
