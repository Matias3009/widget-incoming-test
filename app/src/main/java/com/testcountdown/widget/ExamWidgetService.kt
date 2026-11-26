package com.testcountdown.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import android.widget.RemoteViewsService

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

    override fun onCreate() {}

    override fun onDataSetChanged() {
        now = System.currentTimeMillis()
        exams = repo.getUpcoming(now)
        settings = repo.settings
    }

    override fun onDestroy() {}

    override fun getCount(): Int = exams.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        val exam = exams.getOrNull(position) ?: return views
        val density = context.resources.displayMetrics.density
        val bitmap = PillRenderer.render(exam, now, settings, rowWidthPx(density), density)
        views.setImageViewBitmap(R.id.pill, bitmap)
        views.setContentDescription(
            R.id.pill, "${exam.label}: ${Urgency.formatTimeLeft(exam.timeLeft(now))} left",
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
        // Minus the widget's inner padding (10dp on each side).
        return ((widthDp - 20) * density).toInt()
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = exams.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
