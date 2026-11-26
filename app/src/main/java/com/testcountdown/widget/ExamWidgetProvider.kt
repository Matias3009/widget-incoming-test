package com.testcountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews

class ExamWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle,
    ) {
        // Widget was resized: redraw the bars for the new width.
        updateWidget(context, manager, appWidgetId)
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.exam_list)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> updateAll(context)
        }
    }

    companion object {

        /** Refresh every widget instance (call after adding/editing tests or changing settings). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ExamWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.exam_list)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Header like "3 tests" / "3 pruebas" (refreshed with every update).
            val count = ExamRepository(context).getUpcoming().size
            views.setTextViewText(
                R.id.header,
                if (count == 0) context.getString(R.string.widget_title)
                else context.resources.getQuantityString(R.plurals.widget_count, count, count),
            )

            // Scrollable list, filled by ExamWidgetService.
            val serviceIntent = Intent(context, ExamWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.exam_list, serviceIntent)
            views.setEmptyView(R.id.exam_list, R.id.empty_view)

            // Tapping the header, the empty text or any row opens the app.
            val open = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.header, openPending)
            views.setOnClickPendingIntent(R.id.empty_view, openPending)
            val template = PendingIntent.getActivity(
                context, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            views.setPendingIntentTemplate(R.id.exam_list, template)

            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
