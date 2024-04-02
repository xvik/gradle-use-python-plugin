package ru.vyarus.gradle.plugin.python

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import ru.vyarus.gradle.plugin.python.cmd.Venv
import ru.vyarus.gradle.plugin.python.cmd.Virtualenv
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.cmd.env.GradleEnvironment
import ru.vyarus.gradle.plugin.python.service.EnvService
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Base class for Gradle TestKit based tests.
 * Useful for full-cycle and files manipulation testing.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
abstract class AbstractKitTest extends Specification {

    boolean debug
    boolean isWin = Os.isFamily(Os.FAMILY_WINDOWS)

    @TempDir File testProjectDir
    File buildFile

    def setup() {
        buildFile = file('build.gradle')
        // jacoco coverage support
        fileFromClasspath('gradle.properties', 'testkit-gradle.properties')
    }

    def build(String file) {
        buildFile << file
    }

    File file(String path) {
        new File(testProjectDir, path)
    }

    File fileFromClasspath(String toFile, String source) {
        File target = file(toFile)
        target.parentFile.mkdirs()
        target.withOutputStream {
            it.write((getClass().getResourceAsStream(source) ?: getClass().classLoader.getResourceAsStream(source)).bytes)
        }
        target
    }

    /**
     * Enable it and run test with debugger (no manual attach required). Not always enabled to speed up tests during
     * normal execution.
     */
    def debug() {
        debug = true
    }

    String projectName() {
        return testProjectDir.getName()
    }

    GradleRunner gradle(File root, String... commands) {
        GradleRunner.create()
                .withProjectDir(root)
                .withArguments((commands + ['--stacktrace']) as String[])
                .withPluginClasspath()
                .withDebug(debug)
                .forwardOutput()
    }

    GradleRunner gradle(String... commands) {
        gradle(testProjectDir, commands)
    }

    BuildResult run(String... commands) {
        return gradle(commands).build()
    }

    BuildResult runFailed(String... commands) {
        return gradle(commands).buildAndFail()
    }

    BuildResult runVer(String gradleVersion, String... commands) {
        println 'Running with GRADLE ' + gradleVersion
        return gradle(commands).withGradleVersion(gradleVersion).build()
    }

    BuildResult runFailedVer(String gradleVersion, String... commands) {
        println 'Running with GRADLE ' + gradleVersion
        return gradle(commands).withGradleVersion(gradleVersion).buildAndFail()
    }

    protected String unifyString(String input) {
        return input
        // cleanup win line break for simpler comparisons
                .replace("\r", '')
    }

    String unifyStats(String text) {
        return unifyString(text)
                .replaceAll(/\d{2}:\d{2}:\d{2}:\d{3}/, '11:11:11:111')
                .replaceAll(/(\d\.?)+(ms|s)\s+/, '11ms                ')
                .replaceAll(/11ms\s+\(overall\)/, '11ms (overall)')
                .replaceAll(/ +\/[a-z_]{2,} +/, "   /test_container    ")
                // workaround for windows paths
                .replace('\\', '/')
    }

    // custom virtualenv to use for simulations
    Virtualenv env(String path = '.gradle/python', String binary = null) {
        new Virtualenv(gradleEnv(ProjectBuilder.builder()
                .withProjectDir(testProjectDir).build()), null, binary, path)
    }

    Venv venv(String path = '.gradle/python', String binary = null) {
        new Venv(gradleEnv(ProjectBuilder.builder()
                .withProjectDir(testProjectDir).build()), null, binary, path)
    }

    Environment gradleEnv() {
        gradleEnv(ProjectBuilder.builder().build())
    }

    Environment gradleEnv(Project project) {
        GradleEnvironment.create(project, "gg", project.gradle.sharedServices.registerIfAbsent(
                'pythonEnvironmentService', EnvService, spec -> {
            EnvService.Params params = spec.parameters as EnvService.Params
            params.printStats.set(false)
            params.debug.set(false)
        }
        ), project.provider { false })
    }
}
