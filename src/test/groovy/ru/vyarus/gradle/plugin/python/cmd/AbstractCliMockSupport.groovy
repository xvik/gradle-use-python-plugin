package ru.vyarus.gradle.plugin.python.cmd

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.file.PathToFileResolver
import org.gradle.process.internal.DefaultExecAction
import org.gradle.util.ConfigureUtil
import ru.vyarus.gradle.plugin.python.util.ExecRes
import ru.vyarus.gradle.plugin.python.util.TestLogger
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
abstract class AbstractCliMockSupport extends Specification {

    Project project
    TestLogger logger


    void setup() {
        project = Stub(Project)
        logger = new TestLogger()
        project.getLogger()  >>  { logger }
    }

    void mockExec(Project project, String output, int res) {
        project.exec(_) >> {Closure spec ->
            DefaultExecAction act = ConfigureUtil.configure(spec, new DefaultExecAction(Stub(PathToFileResolver)))
            OutputStream os = act.standardOutput
            os.write(output.bytes)
            return new ExecRes(res)
        }
    }
}
