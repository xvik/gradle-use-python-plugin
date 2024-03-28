package ru.vyarus.gradle.plugin.python.cmd

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.file.PathToFileResolver
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.gradle.process.internal.DefaultExecSpec
import ru.vyarus.gradle.plugin.python.cmd.env.Environment
import ru.vyarus.gradle.plugin.python.cmd.env.GradleEnvironment
import ru.vyarus.gradle.plugin.python.service.stat.PythonStat
import ru.vyarus.gradle.plugin.python.service.value.CacheValueSource
import ru.vyarus.gradle.plugin.python.util.ExecRes
import ru.vyarus.gradle.plugin.python.util.TestLogger
import spock.lang.Specification
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
abstract class AbstractCliMockSupport extends Specification {

    // used to overcome manual file existence check on win
    @TempDir
    File dir

    Project project
    TestLogger logger
    private boolean execMocked
    Map<Closure<String>, String> execCases = [:]
    Map<String, Object> extraProps = [:]

    boolean isWin = Os.isFamily(Os.FAMILY_WINDOWS)

    File file(String path) {
        new File(dir, path)
    }

    void setup() {
        project = Stub(Project)
        logger = new TestLogger()
        project.getLogger() >> { logger }
        project.getProjectDir() >> { dir }
        project.file(_) >> { new File(dir, it[0]) }
        project.getRootProject() >> { project }
        project.findProperty(_ as String) >> { args -> extraProps.get(args[0]) }
        // required for GradleEnvironment
        ObjectFactory objects = Stub(ObjectFactory)
        ExecOperations exec = Stub(ExecOperations)
        exec.exec(_) >> { project.exec it[0] as Action }
        FileOperations fs = Stub(FileOperations)
        fs.file(_) >> { project.file(it[0]) }
        objects.newInstance(_, _) >> { args ->
            List params = [exec, fs]
            params.addAll(args[1] as Object[])
            // have to use special class because GradleEnvironment is abstract (assume gradle injection)
            GradleEnv.newInstance(params as Object[])
        }
        project.getObjects() >> { objects }

        ProviderFactory providers = Stub(ProviderFactory)
        providers.of(_, _) >> { args ->
            Class cls = args[0] as Class
            if (CacheValueSource.isAssignableFrom(cls)) {
                return { [:] } as Provider
            } else {
                return { [] } as  Provider
            }
        }
        project.getProviders() >> { providers }

        def ext = Stub(ExtensionContainer)
        project.getExtensions() >> { ext }
        def props = Stub(ExtraPropertiesExtension)
        ext.getExtraProperties() >> { props }
        props.set(_ as String, _) >> { args -> extraProps.put(args[0], args[1]) }
        props.get(_ as String) >> { args -> extraProps.get(args[0]) }
        props.has(_ as String) >> { args -> extraProps.containsKey(args[0]) }
    }

    Environment gradleEnv() {
        gradleEnv(project)
    }

    Environment gradleEnv(Project project) {
        GradleEnvironment.create(project, "gg", {} as Provider, { false } as Provider)
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
        project.exec(_) >> { Action<ExecSpec> action ->
            ExecSpec spec = new DefaultExecSpec(Stub(PathToFileResolver))
            action.execute(spec)
            String cmd = "${spec.executable} ${spec.args.join(' ')}"
            println ">> Mocked exec: $cmd"
            String out = output
            execCases.each { k, v ->
                if (k.call(cmd)) {
                    println ">> Special exec case detected, output become: $v"
                    out = v
                }
            }
            if (out == output) {
                println ">> Default execution, output: $out"
            }

            OutputStream os = spec.standardOutput
            if (out) {
                os.write(out.bytes)
            }
            return new ExecRes(res)
        }
    }

    static class GradleEnv extends GradleEnvironment {
        ExecOperations exec
        FileOperations fs

        GradleEnv(ExecOperations exec, FileOperations fs, Logger logger, File projectDir, File rootDir, String rootName,
                  String projectPath, String taskName,
                  Provider<Map<String, Object>> globalCache,
                  Provider<Map<String, Object>> projectCache,
                  Provider<List<PythonStat>> stats,
                  Provider<Boolean> debug) {
            super(logger, projectDir, rootDir, rootName, projectPath, taskName, globalCache, projectCache, stats, debug)
            this.exec = exec
            this.fs = fs
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
}
