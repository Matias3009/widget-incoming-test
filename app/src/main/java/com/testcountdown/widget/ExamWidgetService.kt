package com.testcountdown.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlin.math.roundToInt

class ExamWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return ExamViewsFactory(applicationContext, id)
    }
}

private class ExamViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {

    private val repo = ExamRepository(context)
    private var exams: List<Exam> = emptyList()
    private var settings = WidgetSettings()
    private var now = 0L
    private var pillHeightDp = DEFAULT_PILL_DP

    override fun onCreate() {}

    override fun onDataSetChanged() {
        now = System.currentTimeMillis()
        exams = repo.getUpcoming(now)
        settings = repo.settings
        pillHeightDp = computePillHeightDp()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = exams.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        val exam = exams.getOrNull(position) ?: return views
        val res = context.resources
        val density = res.displayMetrics.density
        val bitmap = PillRenderer.render(
            exam, now, settings, rowWidthPx(density), density,
            outsideTextColor = context.getColor(R.color.widget_text),
            heightDp = pillHeightDp,
        )
        views.setImageViewBitmap(R.id.pill, bitmap)
        views.setContentDescription(
            R.id.pill,
            res.getString(R.string.time_left_description, exam.label, Urgency.formatTimeLeft(exam.timeLeft(now))),
        )
        views.setOnClickFillInIntent(R.id.pill, Intent())
        return views
    }

    /** Width available for a row, from the widget's current size. */
    private fun rowWidthPx(density: Float): Int {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val widthDp = if (portrait) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        }.takeIf { it > 0 } ?: 250
        // Minus the widget's inner padding on both sides.
        val paddingPx = 2 * context.resources.getDimensionPixelSize(R.dimen.widget_padding_h)
        return (widthDp * density).toInt() - paddingPx
    }

    /**
     * Picks the bar height so a whole number of rows exactly fills the list area
     * (no half-cut row at the bottom), with rows close to [PREFERRED_ROW_DP] tall,
     * like the rows of the launcher's own list widgets.
     */
    private fun computePillHeightDp(): Float {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val heightDp = if (portrait) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        }
        if (heightDp <= 0) return DEFAULT_PILL_DP

        // Must mirror widget_layout.xml: 12dp padding at the top and bottom.
        val listDp = heightDp - 12f - 12f - 2f // 2dp safety margin
        val rows = (listDp / PREFERRED_ROW_DP).roundToInt().coerceAtLeast(1)
        val rowDp = (listDp / rows).coerceIn(MIN_ROW_DP, MAX_ROW_DP)
        return rowDp - ROW_PADDING_DP
    }

    private companion object {
        const val DEFAULT_PILL_DP = 38f
        const val PREFERRED_ROW_DP = 42f
        const val MIN_ROW_DP = 36f
        const val MAX_ROW_DP = 54f
        const val ROW_PADDING_DP = 8f // widget_item.xml: 4dp top + 4dp bottom
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = exams.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
