package de.projektastra.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

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
        val result = goAsync()
        AstraWidgetUpdater.updateWidgets(context, appWidgetIds) { result.finish() }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val result = goAsync()
        AstraWidgetUpdater.updateWidgets(context, intArrayOf(appWidgetId)) { result.finish() }
    }
}
