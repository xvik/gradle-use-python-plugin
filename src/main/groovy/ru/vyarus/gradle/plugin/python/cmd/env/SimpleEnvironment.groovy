package ru.vyarus.gradle.plugin.python.cmd.env

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.gradle.testfixtures.ProjectBuilder
import ru.vyarus.gradle.plugin.python.service.stat.PythonStat

import java.util.concurrent.ConcurrentHashMap

/**
 * Environment implementation using fake project (in current directory). Might be used for direct python and pip
 * tools execution in tests (use global tools). For example, to uninstall global pip package before test.
 *
 * @author Vyacheslav Rusakov
 * @since 04.04.2024
 */
@CompileStatic
class SimpleEnvironment extends GradleEnvironment {

    private final ExecOperations exec
    private final FileOperations fs

    SimpleEnvironment() {
        this(new File(''), false)
    }

    SimpleEnvironment(File projectDir, boolean debug) {
        this(ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build(), debug)
    }

    SimpleEnvironment(Project project, boolean debug) {
        super(project.logger,
                project.projectDir,
                project.projectDir,
                'local', ':', 'dummy',
                null,
                { new ConcurrentHashMap<>() } as Provider,
                { [] } as Provider,
                { debug } as Provider
        )
        this.exec = (project as DefaultProject).services.get(ExecOperations)
        this.fs = (project as DefaultProject).services.get(FileOperations)
    }

    Map<String, Object> getCache() {
        return cacheProject.get()
    }

    List<PythonStat> getStats() {
        return super.stats.get()
    }

    @Override
    protected ExecOperations getExec() {
        return exec
    }

    @Override
    protected FileOperations getFs() {
        return fs
    }
}
