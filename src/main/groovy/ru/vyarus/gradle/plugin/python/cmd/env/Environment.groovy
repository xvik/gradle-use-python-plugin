package ru.vyarus.gradle.plugin.python.cmd.env

import org.gradle.api.logging.Logger

import java.util.function.Supplier

/**
 * Environment-specific apis provider. Object used as lightweight alternative to gradle {@link org.gradle.api.Project}
 * (which was used before), because project is not compatible with configuration cache.
 *
 * @author Vyacheslav Rusakov
 * @since 15.03.2024
 */
interface Environment {

    Logger getLogger()

    /**
     * @return root project name
     */
    String getRootName()

    /**
     * @return current project path (e.g. :mod1:sub1) to uniquely identify project
     */
    String getProjectPath()

    /**
     * @return root project directory
     */
    File getRootDir()

    /**
     * @return current project directory (might be root or sub module)
     */
    File getProjectDir()

    /**
     * @param path absolute or relative path to file (for current project)
     * @return file, resolved relative to current project
     */
    File file(String path)

    /**
     * @param path absolute path
     * @return path relative for current project
     */
    String relativePath(String path)

    String relativePath(File file)

    /**
     * Execute command (external process).
     *
     * @param cmd command
     * @param out output stream
     * @param err errors stream
     * @param workDir work directory (may be null)
     * @param envVars environment variables (may be null)
     * @return exit code
     */
    int exec(String[] cmd, OutputStream out, OutputStream err, String workDir, Map<String, Object> envVars)

    /**
     * Compute value or get from project-wide cache. Used to cache values within one project. Unifies cache between
     * {@link ru.vyarus.gradle.plugin.python.cmd.Python} instances (created independently for each task).
     * <p>
     * Used as a replacement for project external property, which is impossible to use due to new limitation of
     * not using {@link org.gradle.api.Project} inside task action.
     *
     * @param key cache key (case sensitive)
     * @param value value supplier (used when nothing stored in cache), may be null
     * @return project cache
     */
    <T> T projectCache(String key, Supplier<T> value)

    /**
     * Compute value or get from global-wide cache. Unique cache for all projects in multi-module build
     * (to cache values, common for all modules and avoid redundant python calls)
     * <p>
     * Used as a replacement for project external property, which is impossible to use due to new limitation of
     * not using {@link org.gradle.api.Project} inside task action.
     *
     * @param key cache key (case sensitive)
     * @param value value supplier (used when nothing stored in cache), may be null
     * @return project cache
     */
    <T> T globalCache(String key, Supplier<T> value)

    /**
     * Update project cache value (even if it already contains value),
     *
     * @param key cache key
     * @param value value
     */
    void updateProjectCache(String key, Object value)

    /**
     * Update global cache value (even if it already contains value).
     *
     * @param key cache key
     * @param value value
     */
    void updateGlobalCache(String key, Object value)
}
