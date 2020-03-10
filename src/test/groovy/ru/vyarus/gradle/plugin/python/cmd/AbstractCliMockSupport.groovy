package ru.vyarus.gradle.plugin.python.cmd

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.initialization.DefaultBuildCancellationToken
import org.gradle.internal.file.PathToFileResolver
import org.gradle.process.internal.DefaultExecAction
import org.gradle.util.ConfigureUtil
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.vyarus.gradle.plugin.python.util.ExecRes
import ru.vyarus.gradle.plugin.python.util.TestLogger
import spock.lang.Specification

import java.util.concurrent.Executors

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
    private boolean execMocked
    Map<Closure<String>, String> execCases = [:]

    boolean isWin = Os.isFamily(Os.FAMILY_WINDOWS)

    File file(String path) {
        new File(dir.root, path)
    }

    void setup() {
        project = Stub(Project)
        logger = new TestLogger()
        project.getLogger() >> { logger }
        project.getProjectDir() >> { dir.root }
        project.file(_) >> { new File(dir.root, it[0]) }
    }

    // use to provide specialized output for executed commands
    // (e.g. under pip tests to cactch python virtualenv detection)
    // closure accepts called command line
    void execCase(Closure<String> closure, String output) {
        execCases.put(closure, output)
    }

    void mockExec(Project project, String output, int res) {
        assert !execMocked, "Exec can be mocked just once!"
        // check execution with logs without actual execution
        project.exec(_) >> { Closure spec ->
            DefaultExecAction act = ConfigureUtil.configure(spec,
                    new DefaultExecAction(Stub(PathToFileResolver),
                            Executors.newSingleThreadExecutor(),
                            new DefaultBuildCancellationToken()))

            String cmd = "${act.executable} ${act.args.join(' ')}"
            println ">> Mocked exec: $cmd"
            String out = output
            execCases.each { k, v ->
                if (k.call(cmd)) {
                    println ">> Special exec case detected, output become: $v"
                    out = v
                }
            }
            if (out==output) {
                println ">> Default execution, output: $out"
            }

            OutputStream os = act.standardOutput
            if (out) {
                os.write(out.bytes)
            }
            return new ExecRes(res)
        }
    }
}
