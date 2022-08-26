package ru.vyarus.gradle.plugin.python.util

import groovy.transform.CompileStatic
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.python.PythonExtension

/**
 * Read requirements file and convert it into plugin's modules declaration syntax (the same as modules declared in
 * {@link ru.vyarus.gradle.plugin.python.PythonExtension#pip(java.lang.String [ ])}).
 *
 * @author Vyacheslav Rusakov
 * @since 24.08.2022
 * @see <a href="https://pip.pypa.io/en/stable/user_guide/#requirements-files">requirements files</a>
 * @see <a href="https://pip.pypa.io/en/stable/reference/requirements-file-format/#requirements-file-format">format</a>
 */
@CompileStatic
class RequirementsReader {

    /**
     * Searches for requirements file, counting that requirements files support could be disabled. File is searched
     * relative to project root (in case of module - module root). File is not searched for submodule inside root
     * project to avoid situation when all modules read requirements from root which must be using different
     * set of dependencies. If required, root file could be always manually configured for sub modules.
     *
     * @param project project
     * @param requirements extension
     * @return found file or null
     */
    static File find(Project project, PythonExtension.Requirements requirements) {
        if (!requirements.use) {
            return null
        }
        File reqs = project.file(requirements.file)
        return reqs.exists() ? reqs : null
    }

    /**
     * Reads module declarations from requirements file. Does not perform any validations: it is assumed to
     * be used for plugin input which would complain if some declaration is incorrect.
     * <p>
     * Returns all non empty and non-comment lines. Only replace '==' into ':' to convert from python declaration
     * syntax into plugin syntax.
     * <p>
     * NOTE: only not quite correct vcs modules syntax is supported: its egg part must contain version (which is wrong
     * for pure pip declaration). Anyway, that is the only way for plugin to know vcs module version and
     * correctly apply up-to-date checks.
     *
     * @param file requirements file
     * @return module declarations from requirements file or empty list
     */
    static List<String> read(File file) {
        if (!file || !file.exists()) {
            return Collections.emptyList()
        }

        // requirements file may use different encoding, but its intentionally not supported
        List<String> res = []
        file.readLines('utf-8').each {
            String line = it.trim()
            if (line && !line.startsWith('#')) {
                // translate python syntax into "plugin syntax" (required only for simple packages)
                res.add(line.replace('==', ':'))
            }
        }
        return res
    }

    /**
     * Returns path, relative for current project if file located somewhere inside root project. Otherwise returns
     * absolute file path (file located outside project dir)
     *
     * @param project current project
     * @param file file to get path of
     * @return relative file path if file is located inside project or absolute path
     */
    static String relativePath(Project project, File file) {
        if (file.canonicalPath.startsWith(project.rootProject.rootDir.canonicalPath)) {
            return project.relativePath(file)
        }
        return file.canonicalPath
    }
}
