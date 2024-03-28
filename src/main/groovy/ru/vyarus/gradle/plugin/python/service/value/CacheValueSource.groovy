package ru.vyarus.gradle.plugin.python.service.value

import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import ru.vyarus.gradle.plugin.python.service.EnvService

/**
 * Required to prevent configuration cache from storing inner cache maps (which would make all python instances
 * depend on its own cache map, making cache useless).
 *
 * @author Vyacheslav Rusakov
 * @since 26.03.2024
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class CacheValueSource implements ValueSource<Map<String, Object>, CacheParams> {

    Map<String, Object> obtain() {
        return parameters.service.get()
                .getCache(parameters.project.get())
    }

    interface CacheParams extends ValueSourceParameters {
        Property<EnvService> getService()
        Property<String> getProject()
    }
}
