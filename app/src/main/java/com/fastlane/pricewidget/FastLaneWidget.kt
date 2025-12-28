package com.fastlane.pricewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class FastLaneWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.fastlane.pricewidget.REFRESH_WIDGET"
        const val ACTION_AUTO_UPDATE = "com.fastlane.pricewidget.AUTO_UPDATE"
        private const val AUTO_UPDATE_REQUEST_CODE = 1001

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, FastLaneWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, FastLaneWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }

        /**
         * Update all widgets with a specific price
         */
        fun updateWidgetWithPrice(context: Context, price: Int) {
            val instance = FastLaneWidget()
            instance.updateWidgetWithPriceInternal(context, price)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Update widget when size changes
        fetchPriceAndUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                fetchPriceAndUpdate(context)
            }
            ACTION_AUTO_UPDATE -> {
                // Auto-update triggered by AlarmManager
                if (isActiveHours(context)) {
                    fetchPriceAndUpdate(context)
                } else {
                    // Not in active hours anymore, stop scheduling
                    stopScheduledUpdates(context)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleUpdates(context)
        fetchPriceAndUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopScheduledUpdates(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Get widget size
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        
        // Determine which layout to use based on size
        // 1x1 = approximately 40-80dp, 2x2 = approximately 110-180dp
        val layoutId = if (minWidth < 100 || minHeight < 100) {
            R.layout.widget_layout_small  // 1x1
        } else {
            R.layout.widget_layout  // 2x2+
        }
        
        val views = RemoteViews(context.packageName, layoutId)
        
        // Load and display last saved price if available
        val lastPrice = WidgetPreferences.getLastPrice(context)
        if (lastPrice > 0) {
            views.setTextViewText(R.id.price_text, lastPrice.toString())
            
            // Set color based on saved price
            val threshold1 = WidgetPreferences.getLowToMediumThreshold(context)
            val threshold2 = WidgetPreferences.getMediumToHighThreshold(context)
            val themeName = WidgetPreferences.getColorTheme(context)
            
            val colorHex = when {
                lastPrice <= threshold1 -> when (themeName) {
                    "vibrant" -> "#4CAF50"
                    "dark" -> "#2E7D32"
                    "minimal" -> "#E8F5E9"
                    "neon" -> "#00FF88"
                    else -> "#A8E6CF"
                }
                lastPrice <= threshold2 -> when (themeName) {
                    "vibrant" -> "#FFC107"
                    "dark" -> "#F57C00"
                    "minimal" -> "#FFF8E1"
                    "neon" -> "#FFFF00"
                    else -> "#FFE5B4"
                }
                else -> when (themeName) {
                    "vibrant" -> "#F44336"
                    "dark" -> "#C62828"
                    "minimal" -> "#FFEBEE"
                    "neon" -> "#FF00FF"
                    else -> "#FFB3BA"
                }
            }
            
            val backgroundColor = android.graphics.Color.parseColor(colorHex)
            views.setInt(R.id.widget_container, "setBackgroundColor", backgroundColor)
        }
        
        // Set click listener for manual refresh
        val intent = Intent(context, FastLaneWidget::class.java).apply {
            action = ACTION_REFRESH
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun fetchPriceAndUpdate(context: Context) {
        // Show loading state
        showLoadingState(context)

        Thread {
            try {
                val price = PriceApi.getCurrentPrice()

                // Run UI updates on main thread
                Handler(Looper.getMainLooper()).post {
                    // Update this widget directly
                    updateWidgetWithPriceInternal(context, price)

                    // Also broadcast to save price
                    PriceUpdateReceiver.broadcastPriceUpdate(context, price)
                }
            } catch (e: Exception) {
                android.util.Log.e("FastLaneWidget", "Error fetching price: ${e.message}", e)
                e.printStackTrace()
                // Run error update on main thread
                Handler(Looper.getMainLooper()).post {
                    updateWidgetWithError(context)
                }
            } finally {
                // ALWAYS schedule next update if in active hours, even on error
                // This ensures auto-refresh continues working
                Handler(Looper.getMainLooper()).post {
                    scheduleUpdates(context)
                }
            }
        }.start()
    }

    private fun showLoadingState(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, FastLaneWidget::class.java)
        )

        // Get last saved price to preserve it during loading
        val lastPrice = WidgetPreferences.getLastPrice(context)

        for (appWidgetId in ids) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            val layoutId = if (minWidth < 100 || minHeight < 100) {
                R.layout.widget_layout_small
            } else {
                R.layout.widget_layout
            }

            val views = RemoteViews(context.packageName, layoutId)

            // Keep showing the last price if available instead of "--"
            if (lastPrice > 0) {
                views.setTextViewText(R.id.price_text, lastPrice.toString())

                // Keep the background color based on last price
                val threshold1 = WidgetPreferences.getLowToMediumThreshold(context)
                val threshold2 = WidgetPreferences.getMediumToHighThreshold(context)
                val themeName = WidgetPreferences.getColorTheme(context)

                val colorHex = when {
                    lastPrice <= threshold1 -> when (themeName) {
                        "vibrant" -> "#4CAF50"
                        "dark" -> "#2E7D32"
                        "minimal" -> "#E8F5E9"
                        "neon" -> "#00FF88"
                        else -> "#A8E6CF"
                    }
                    lastPrice <= threshold2 -> when (themeName) {
                        "vibrant" -> "#FFC107"
                        "dark" -> "#F57C00"
                        "minimal" -> "#FFF8E1"
                        "neon" -> "#FFFF00"
                        else -> "#FFE5B4"
                    }
                    else -> when (themeName) {
                        "vibrant" -> "#F44336"
                        "dark" -> "#C62828"
                        "minimal" -> "#FFEBEE"
                        "neon" -> "#FF00FF"
                        else -> "#FFB3BA"
                    }
                }

                val backgroundColor = android.graphics.Color.parseColor(colorHex)
                views.setInt(R.id.widget_container, "setBackgroundColor", backgroundColor)
            }

            // Hide refresh button, show loading spinner in its place
            views.setViewVisibility(R.id.refresh_button, android.view.View.GONE)
            views.setViewVisibility(R.id.refresh_loading, android.view.View.VISIBLE)

            // Update time text to show "מעדכן..." for large layout
            if (layoutId == R.layout.widget_layout) {
                views.setTextViewText(R.id.update_time, "מעדכן...")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun updateWidgetWithPriceInternal(context: Context, price: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, FastLaneWidget::class.java)
        )

        // Check if we should send notification
        PriceNotificationManager.checkAndNotify(context, price)

        for (appWidgetId in ids) {
            // Get widget size to determine layout
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            
            val layoutId = if (minWidth < 100 || minHeight < 100) {
                R.layout.widget_layout_small  // 1x1
            } else {
                R.layout.widget_layout  // 2x2+
            }
            
            val views = RemoteViews(context.packageName, layoutId)

            // Hide loading spinner, show refresh button back
            views.setViewVisibility(R.id.refresh_loading, android.view.View.GONE)
            views.setViewVisibility(R.id.refresh_button, android.view.View.VISIBLE)

            // Update price
            views.setTextViewText(R.id.price_text, price.toString())
            
            // Get current theme colors - inline for simplicity
            val themeName = WidgetPreferences.getColorTheme(context)
            
            // Update background color based on price and thresholds
            val threshold1 = WidgetPreferences.getLowToMediumThreshold(context)
            val threshold2 = WidgetPreferences.getMediumToHighThreshold(context)
            
            // Determine color based on price zone and theme
            val colorHex = when {
                price <= threshold1 -> { // Green zone
                    when (themeName) {
                        "vibrant" -> "#4CAF50"
                        "dark" -> "#2E7D32"
                        "minimal" -> "#E8F5E9"
                        "neon" -> "#00FF88"
                        else -> "#A8E6CF" // pastel
                    }
                }
                price <= threshold2 -> { // Yellow zone
                    when (themeName) {
                        "vibrant" -> "#FFC107"
                        "dark" -> "#F57C00"
                        "minimal" -> "#FFF8E1"
                        "neon" -> "#FFFF00"
                        else -> "#FFE5B4" // pastel
                    }
                }
                else -> { // Red zone
                    when (themeName) {
                        "vibrant" -> "#F44336"
                        "dark" -> "#C62828"
                        "minimal" -> "#FFEBEE"
                        "neon" -> "#FF00FF"
                        else -> "#FFB3BA" // pastel
                    }
                }
            }
            
            val backgroundColor = android.graphics.Color.parseColor(colorHex)
            
            // Set solid background color with rounded corners
            views.setInt(R.id.widget_container, "setBackgroundColor", backgroundColor)
            
            // Update time (only for large layout)
            if (layoutId == R.layout.widget_layout) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale("he", "IL"))
                val currentTime = timeFormat.format(Date())
                views.setTextViewText(R.id.update_time, currentTime)
            }
            
            // Set click listener to open MainActivity (settings)
            val settingsIntent = Intent(context, MainActivity::class.java)
            val settingsPendingIntent = PendingIntent.getActivity(
                context, 0, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, settingsPendingIntent)
            
            // Set click listener for refresh button
            val refreshIntent = Intent(context, FastLaneWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun updateWidgetWithError(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, FastLaneWidget::class.java)
        )

        for (appWidgetId in ids) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            
            val layoutId = if (minWidth < 100 || minHeight < 100) {
                R.layout.widget_layout_small
            } else {
                R.layout.widget_layout
            }
            
            val views = RemoteViews(context.packageName, layoutId)

            // Hide loading spinner, show refresh button back
            views.setViewVisibility(R.id.refresh_loading, android.view.View.GONE)
            views.setViewVisibility(R.id.refresh_button, android.view.View.VISIBLE)

            views.setTextViewText(R.id.price_text, "!")
            
            if (layoutId == R.layout.widget_layout) {
                views.setTextViewText(R.id.update_time, "שגיאה")
            }
            
            val intent = Intent(context, FastLaneWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun isActiveHours(context: Context): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"))
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        // Check if auto-update is enabled
        if (!WidgetPreferences.isAutoUpdateEnabled(context)) {
            return false
        }

        // Get configured hours from preferences
        val startHour = WidgetPreferences.getUpdateStartHour(context)
        val endHour = WidgetPreferences.getUpdateEndHour(context)

        // Get active days from preferences
        val activeDays = WidgetPreferences.getActiveDays(context)

        // Check if today is an active day
        val isDayActive = activeDays.contains(dayOfWeek)

        // Check if current hour is within active time range
        val isActiveTime = hourOfDay in startHour until endHour

        return isDayActive && isActiveTime
    }

    private fun scheduleUpdates(context: Context) {
        try {
            // Cancel any existing alarms first
            stopScheduledUpdates(context)

            // Only schedule if in active hours
            if (!isActiveHours(context)) {
                return
            }

            // Use AlarmManager for persistent scheduling (survives process death)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                android.util.Log.e("FastLaneWidget", "AlarmManager is null, cannot schedule updates")
                return
            }

            val intent = Intent(context, FastLaneWidget::class.java).apply {
                action = ACTION_AUTO_UPDATE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                AUTO_UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Get interval from preferences (in seconds), convert to milliseconds
            val intervalSeconds = WidgetPreferences.getUpdateInterval(context)
            val intervalMillis = intervalSeconds * 1000L

            // Calculate next trigger time
            val triggerAtMillis = System.currentTimeMillis() + intervalMillis

            // Use setExactAndAllowWhileIdle for precise timing even in Doze mode
            // On Android 12+ (API 31+), check if we can schedule exact alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FastLaneWidget", "Error scheduling updates: ${e.message}", e)
        }
    }

    private fun stopScheduledUpdates(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                android.util.Log.e("FastLaneWidget", "AlarmManager is null, cannot stop updates")
                return
            }

            val intent = Intent(context, FastLaneWidget::class.java).apply {
                action = ACTION_AUTO_UPDATE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                AUTO_UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            android.util.Log.e("FastLaneWidget", "Error stopping updates: ${e.message}", e)
        }
    }
}
