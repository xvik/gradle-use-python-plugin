package ru.vyarus.gradle.plugin.python.task.env

import ru.vyarus.gradle.plugin.python.cmd.Pip

/**
 * Virtual environment creation support. No model objects used for virtualenv settings because the entire check task
 * is passed (to re-use its configuration and services).
 * <p>
 * In essence, this class must create environment if required and, eventually, provide different python path
 * to use by plugin.
 * <p>
 * NOTE: virtual environment detection is actually implemented inside {@link ru.vyarus.gradle.plugin.python.cmd.Python}
 * object (by presence of activation script). So only packages using such script are supported (venv, virtualenv).
 *
 * @author Vyacheslav Rusakov
 * @since 01.04.2024
 */
interface EnvSupport {

    /**
     * @return true if environment already exists
     */
    boolean exists()

    /**
     * Create new environment.
     *
     * @param pip pip instance (to check if required package installed)
     * @return true if environment was created
     */
    boolean create(Pip pip)

    /**
     * @return python path to use (inside environment)
     */
    String getPythonPath()
}
