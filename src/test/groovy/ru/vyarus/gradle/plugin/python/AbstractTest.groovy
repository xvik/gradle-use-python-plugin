package ru.vyarus.gradle.plugin.python

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Base class for plugin configuration tests.
 *
 * @author Vyacheslav Rusakov
 * @since 11.11.2017
 */
abstract class AbstractTest extends Specification{

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    Project project(Closure<Project> config = null) {
        Project project = ProjectBuilder.builder()
                .withProjectDir(testProjectDir.root).build()
        if (config) {
            project.configure(project, config)
            project.evaluate()
        }
        return project
    }

    File file(String path) {
        new File(testProjectDir.root, path)
    }

    File fileFromClasspath(String toFile, String source) {
        File target = file(toFile)
        target.parentFile.mkdirs()
        target << getClass().getResourceAsStream(source).text
    }
}
