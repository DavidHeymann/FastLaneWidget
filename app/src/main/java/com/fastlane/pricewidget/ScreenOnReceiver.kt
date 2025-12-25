package com.fastlane.pricewidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                // Refresh home widget
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetComponent = ComponentName(context, FastLaneWidget::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
                
                if (widgetIds.isNotEmpty()) {
                    val updateIntent = Intent(context, FastLaneWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    }
                    context.sendBroadcast(updateIntent)
                }
                
                // Restart floating widget if it was enabled but not running
                if (WidgetPreferences.isFloatingWidgetEnabled(context)) {
                    // Check if service is running
                    val serviceIntent = Intent(context, FloatingWidgetService::class.java)
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
