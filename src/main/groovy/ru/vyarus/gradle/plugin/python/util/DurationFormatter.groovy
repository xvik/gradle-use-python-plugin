package ru.vyarus.gradle.plugin.python.util

import groovy.transform.CompileStatic

/**
 * Copy of gradle's internal {@link org.gradle.internal.time.TimeFormatting} class, which become internal in
 * gradle 4.2 and broke compatibility.
 * <p>
 * Used to pretty print elapsed tile in human readable form.
 *
 * @author Vyacheslav Rusakov
 * @since 21.09.2017
 */
@CompileStatic
class DurationFormatter {
    private static final long MILLIS_PER_SECOND = 1000
    private static final long MILLIS_PER_MINUTE = 60000
    private static final long MILLIS_PER_HOUR = 3600000
    private static final long MILLIS_PER_DAY = 86400000

    private DurationFormatter() {
    }

    /**
     * @param duration duration in milliseconds
     * @return human readable (short) duration
     */
    static String format(long duration) {
        if (duration == 0L) {
            return '0ms'
        }

        StringBuilder result = new StringBuilder()
        long days = (duration / MILLIS_PER_DAY).longValue()
        duration %= MILLIS_PER_DAY
        if (days > 0L) {
            append(result, days, 'd')
        }

        long hours = (duration / MILLIS_PER_HOUR).longValue()
        duration %= MILLIS_PER_HOUR
        if (hours > 0L) {
            append(result, hours, 'h')
        }

        long minutes = (duration / MILLIS_PER_MINUTE).longValue()
        duration %= MILLIS_PER_MINUTE
        if (minutes > 0L) {
            append(result, minutes, 'm')
        }

        boolean secs = false
        if (duration >= MILLIS_PER_SECOND) {
            // if only secs, show rounded value, otherwise get rid of ms
            int secondsScale = result.length() == 0 ? 2 : 0
            append(result,
                    BigDecimal.valueOf(duration)
                            .divide(BigDecimal.valueOf(MILLIS_PER_SECOND))
                            .setScale(secondsScale, 4)
                            .stripTrailingZeros()
                            .toPlainString(),
                    's')
            secs = true
            duration %= MILLIS_PER_SECOND
        }

        if (!secs && duration > 0) {
            result.append(duration + 'ms')
        }
        return result.toString()
    }

    private static void append(StringBuilder builder, Object num, String what) {
        if (builder.length() > 0) {
            builder.append(' ')
        }
        builder.append(num)
        builder.append(what)
    }
}
