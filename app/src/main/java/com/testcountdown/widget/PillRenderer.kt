package com.testcountdown.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils

/**
 * Draws one row of the widget: a rounded "pill" whose width depends on the time left,
 * filled with the urgency color, with "COURSE (n)" and the time left ("3W") on it.
 *
 * Rendered to a Bitmap because RemoteViews can't set an arbitrary width or a dynamic
 * gradient on older Android versions.
 */
object PillRenderer {

    private const val TEXT_ON_PILL = 0xFF2B2B2B.toInt()

    fun render(
        exam: Exam,
        now: Long,
        settings: WidgetSettings,
        widthPx: Int,
        density: Float,
        /** Color for the course name when it is drawn beside the bar (the widget's text color). */
        outsideTextColor: Int,
        /** Bar height; the widget picks it so its rows exactly fill the available height. */
        heightDp: Float = 38f,
    ): Bitmap {
        val heightPx = (heightDp * density).toInt()
        val width = widthPx.coerceAtLeast((120 * density).toInt())
        val bitmap = Bitmap.createBitmap(width, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val left = exam.timeLeft(now)
        val color = Urgency.colorFor(left)
        val timeText = Urgency.formatTimeLeft(left)
        val pad = 14 * density

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16 * density
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.color = TEXT_ON_PILL
        }

        val timeWidth = timePaint.measureText(timeText)
        val labelWidth = labelPaint.measureText(exam.label)

        // Bar width from the time left (and the "less time = bigger/smaller" option),
        // never smaller than what the time label needs.
        val minPill = timeWidth + 2 * pad
        val pillWidth = (width * Urgency.widthFraction(left, settings))
            .coerceAtLeast(minPill)
            .coerceAtMost(width.toFloat())

        // Pill with a soft horizontal gradient: a lighter tint on the left to the urgency color on the right.
        val rect = RectF(0f, 0f, pillWidth, heightPx.toFloat())
        val radius = rect.height() / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, 0f, rect.right, 0f,
                Urgency.lerpColor(color, 0xFFFFFFFF.toInt(), 0.40f), color,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, radius, radius, fill)

        val baseline = heightPx / 2f - (labelPaint.descent() + labelPaint.ascent()) / 2f

        // Time left, right-aligned inside the pill.
        canvas.drawText(timeText, rect.right - pad - timeWidth, baseline, timePaint)

        // Course label: inside the pill if it fits, otherwise to the right of it.
        val insideSpace = pillWidth - timeWidth - 3 * pad
        if (labelWidth <= insideSpace) {
            labelPaint.color = TEXT_ON_PILL
            canvas.drawText(exam.label, rect.left + pad, baseline, labelPaint)
        } else {
            labelPaint.color = outsideTextColor
            val x = pillWidth + pad / 2
            val text = TextUtils.ellipsize(exam.label, labelPaint, width - x, TextUtils.TruncateAt.END)
            canvas.drawText(text, 0, text.length, x, baseline, labelPaint)
        }
        return bitmap
    }
}
