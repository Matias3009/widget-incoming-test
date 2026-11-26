package com.testcountdown.widget

import com.testcountdown.widget.Urgency.DAY
import com.testcountdown.widget.Urgency.HOUR
import com.testcountdown.widget.Urgency.WEEK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrgencyTest {

    @Test fun formatsLikeTheSketch() {
        assertEquals("3W", Urgency.formatTimeLeft(3 * WEEK + 2 * DAY))
        assertEquals("2D", Urgency.formatTimeLeft(2 * DAY + 5 * HOUR))
        assertEquals("10D", Urgency.formatTimeLeft(10 * DAY))
        assertEquals("3H", Urgency.formatTimeLeft(3 * HOUR + 20 * 60_000))
        assertEquals("<1H", Urgency.formatTimeLeft(20 * 60_000))
    }

    @Test fun colors() {
        assertEquals(Urgency.GREEN, Urgency.colorFor(3 * WEEK))
        assertEquals(Urgency.GREEN, Urgency.colorFor(2 * WEEK))
        assertEquals(Urgency.YELLOW, Urgency.colorFor(2 * DAY))
        assertEquals(Urgency.RED, Urgency.colorFor(1))
        // In between: a blend, neither endpoint
        val mid = Urgency.colorFor(8 * DAY)
        assertTrue(mid != Urgency.GREEN && mid != Urgency.YELLOW)
        val orange = Urgency.colorFor(DAY)
        assertTrue(orange != Urgency.YELLOW && orange != Urgency.RED)
    }

    @Test fun widthFollowsTheOption() {
        val bigger = WidgetSettings(lessTimeIsBigger = true, scaleWeeks = 8)
        val smaller = bigger.copy(lessTimeIsBigger = false)
        assertTrue(Urgency.widthFraction(DAY, bigger) > Urgency.widthFraction(3 * WEEK, bigger))
        assertTrue(Urgency.widthFraction(DAY, smaller) < Urgency.widthFraction(3 * WEEK, smaller))
        assertEquals(1f, Urgency.widthFraction(0, bigger), 1e-4f)
        assertEquals(Urgency.MIN_FRACTION, Urgency.widthFraction(20 * WEEK, bigger), 1e-4f)
    }
}
