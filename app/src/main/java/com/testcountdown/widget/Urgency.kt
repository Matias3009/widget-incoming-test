package com.testcountdown.widget

import kotlin.math.sqrt

/**
 * The rules from the sketch:
 *  - "3W" = 3 weeks, "2D" = 2 days, "3H" = 3 hours
 *  - more than 2 weeks left  -> green
 *  - between 2 weeks and 2 days -> gradient from green to yellow
 *  - less than 2 days left   -> gradient from yellow to red
 */
object Urgency {

    const val HOUR = 60L * 60L * 1000L
    const val DAY = 24L * HOUR
    const val WEEK = 7L * DAY

    const val GREEN = 0xFF43A047.toInt()
    const val YELLOW = 0xFFFDD835.toInt()
    const val RED = 0xFFE53935.toInt()
    const val GREY = 0xFF9E9E9E.toInt()

    private const val GREEN_UNTIL = 2 * WEEK   // >= this: plain green
    private const val YELLOW_AT = 2 * DAY      // green->yellow ends here, yellow->red starts

    /** Smallest bar, as a fraction of the available width (so the time label always fits). */
    const val MIN_FRACTION = 0.32f

    /** Short label: "5W", "3D", "7H", "<1H". */
    fun formatTimeLeft(millis: Long): String = when {
        millis <= 0 -> "DONE"
        millis >= 2 * WEEK -> "${millis / WEEK}W"
        millis >= DAY -> "${millis / DAY}D"
        millis >= HOUR -> "${millis / HOUR}H"
        else -> "<1H"
    }

    fun colorFor(millis: Long): Int = when {
        millis <= 0 -> GREY
        millis >= GREEN_UNTIL -> GREEN
        millis >= YELLOW_AT -> {
            // 0 at 2 weeks (green) .. 1 at 2 days (yellow)
            val f = (GREEN_UNTIL - millis).toFloat() / (GREEN_UNTIL - YELLOW_AT)
            lerpColor(GREEN, YELLOW, f)
        }
        else -> {
            // 0 at 2 days (yellow) .. 1 at 0 (red)
            val f = (YELLOW_AT - millis).toFloat() / YELLOW_AT
            lerpColor(YELLOW, RED, f)
        }
    }

    /**
     * Bar width as a fraction (MIN_FRACTION..1) of the available width.
     * A square-root scale is used so differences between close tests (days/hours)
     * stay visible even when the scale spans several weeks.
     */
    fun widthFraction(millis: Long, settings: WidgetSettings): Float {
        val scale = settings.scaleWeeks.coerceAtLeast(1) * WEEK
        val r = sqrt((millis.coerceIn(0, scale)).toDouble() / scale).toFloat() // 0 = now, 1 = far
        val t = if (settings.lessTimeIsBigger) 1f - r else r
        return MIN_FRACTION + (1f - MIN_FRACTION) * t
    }

    fun lerpColor(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        fun ch(c: Int, shift: Int) = (c shr shift) and 0xFF
        fun mix(shift: Int) = (ch(from, shift) + (ch(to, shift) - ch(from, shift)) * f).toInt()
        return (mix(24) shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
