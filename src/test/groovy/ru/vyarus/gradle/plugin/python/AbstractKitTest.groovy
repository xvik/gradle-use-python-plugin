package ru.vyarus.gradle.plugin.python

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv
import spock.lang.Specification

/**
 * Base class for Gradle TestKit based tests.
 * Useful for full-cycle and files manipulation testing.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
abstract class AbstractKitTest extends Specification {

    boolean isWin = Os.isFamily(Os.FAMILY_WINDOWS)

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        // jacoco coverage support
        fileFromClasspath('gradle.properties','testkit-gradle.properties')
    }

    def build(String file) {
        buildFile << file
    }

    File file(String path) {
        new File(testProjectDir.root, path)
    }

    File fileFromClasspath(String toFile, String source) {
        File target = file(toFile)
        target.parentFile.mkdirs()
        target << getClass().classLoader.getResourceAsStream(source).text
    }

    /**
     * Allow debug TestKit vm execution. After vm start it will wait for debug connection and continue processing after.
     * (the same effect could be achieved with GradleRunner.withDebug(true) method)
     */
    def debug() {
        file('gradle.properties') << "\norg.gradle.jvmargs=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
    }

    String projectName() {
        return testProjectDir.root.getName()
    }

    GradleRunner gradle(String... commands) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments((commands + ['--stacktrace']) as String[])
                .withPluginClasspath()
                .forwardOutput()
    }

    BuildResult run(String... commands) {
        return gradle(commands).build()
    }

    BuildResult runFailed(String... commands) {
        return gradle(commands).buildAndFail()
    }

    BuildResult runVer(String gradleVersion, String... commands) {
        return gradle(commands).withGradleVersion(gradleVersion).build()
    }

    BuildResult runFailedVer(String gradleVersion, String... commands) {
        return gradle(commands).withGradleVersion(gradleVersion).buildAndFail()
    }

    // custom virtualenv to use for simulations
    Virtualenv env(String path = '.gradle/python') {
        new Virtualenv(ProjectBuilder.builder()
                .withProjectDir(testProjectDir.root).build(), path)
    }
}
