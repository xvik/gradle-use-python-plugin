package ru.vyarus.gradle.plugin.python.cmd.env

import org.gradle.api.logging.Logger

import java.util.function.Supplier

/**
 * Environment-specific apis provider. Object used as lightweight alternative to gradle {@link org.gradle.api.Project}
 * (which was used before), because project is not compatible with configuration cache.
 * <p>
 * NOTE: configuration cache stores entire objects (so they are created only when configuration cache is not enabled).
 *
 * @author Vyacheslav Rusakov
 * @since 15.03.2024
 */
interface Environment {

    Logger getLogger()

    /**
     * Same as {@code project.rootProject.name}.
     *
     * @return root project name
     */
    String getRootName()

    /**
     * Same as {@code project.path}.
     *
     * @return current project path (e.g. :mod1:sub1) to uniquely identify project
     */
    String getProjectPath()

    /**
     * Same as {@code project.rootDir}.
     *
     * @return root project directory
     */
    File getRootDir()

    /**
     * Same as {@code project.projectDir}.
     *
     * @return current project directory (might be root or sub module)
     */
    File getProjectDir()

    /**
     * Same as {@code project.file()}.
     *
     * @param path absolute or relative path to file (for current project)
     * @return file, resolved relative to current project
     */
    File file(String path)

    /**
     * Same as {@code project.relativePath}.
     *
     * @param path absolute path
     * @return path relative for current project
     */
    String relativePath(String path)

    /**
     * Same as {@code project.relativePath}.
     *
     * @param path absolute path
     * @return path relative for current project
     */
    String relativePath(File file)

    /**
     * Rebuild relative or absolute path relative to root project. If path not lying inside root project
     * then path remain absolute.
     *
     * @param path path to convert
     * @return path relative to root project
     */
    String relativeRootPath(String path)

    /**
     * Execute command (external process).
     * Same as {@code project.exec}.
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

    /**
     * Print debug message if debug enabled. Message would include context project and task.
     *
     * @param msg message
     */
    void debug(String msg)

    /**
     * Save command execution stat. Counts only python execution (possible direct docker commands ignored).
     *
     * @param containerName docker container name
     * @param cmd executed command (cleared!)
     * @param workDir working directory (may be null)
     * @param globalPython true to indicate global python call
     * @param start start time
     * @param success execution success
     */
    @SuppressWarnings('ParameterCount')
    void stat(String containerName, String cmd, String workDir, boolean globalPython, long start, boolean success)

    /**
     * Prints cache state (for debug), but only if debug enabled in the root project.
     */
    void printCacheState()
}
