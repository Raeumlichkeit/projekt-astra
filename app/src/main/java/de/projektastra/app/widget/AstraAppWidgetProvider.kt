package de.projektastra.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * AppWidgetProvider for the Projekt Astra Homescreen Widget.
 * Battery-friendly design: strictly zero background GPS and zero background service.
 */
class AstraAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val state = AstraWidgetUpdater.calculateState(context)
        val views = AstraWidgetUpdater.buildRemoteViews(context, state)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
