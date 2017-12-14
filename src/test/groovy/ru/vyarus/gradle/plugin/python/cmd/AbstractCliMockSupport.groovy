package ru.vyarus.gradle.plugin.python.cmd

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.internal.file.PathToFileResolver
import org.gradle.process.internal.DefaultExecAction
import org.gradle.util.ConfigureUtil
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.vyarus.gradle.plugin.python.util.ExecRes
import ru.vyarus.gradle.plugin.python.util.TestLogger
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
abstract class AbstractCliMockSupport extends Specification {

    // used to overcome manual file existence check on win
    @Rule
    TemporaryFolder dir

    Project project
    TestLogger logger

    boolean isWin = Os.isFamily(Os.FAMILY_WINDOWS)

    File file(String path) {
        new File(dir.root, path)
    }

    void setup() {
        project = Stub(Project)
        logger = new TestLogger()
        project.getLogger()  >>  { logger }
        project.getRootDir() >> { dir.root }
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
