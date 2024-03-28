package ru.vyarus.gradle.plugin.python.service.value

import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import ru.vyarus.gradle.plugin.python.service.EnvService
import ru.vyarus.gradle.plugin.python.service.stat.PythonStat

/**
 * Required to prevent configuration cache from storing inner stats list (which would make all python instances
 * depend on its own stats list, hiding stats).
 *
 * @author Vyacheslav Rusakov
 * @since 26.03.2024
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class StatsValueSource implements ValueSource<List<PythonStat>, StatsParams> {

    List<PythonStat> obtain() {
        return parameters.service.get().stats
    }

    interface StatsParams extends ValueSourceParameters {
        Property<EnvService> getService()
    }
}
