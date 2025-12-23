package com.fastlane.pricewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class FastLaneWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.fastlane.pricewidget.REFRESH_WIDGET"
        private val handler = Handler(Looper.getMainLooper())
        private var updateRunnable: Runnable? = null
        
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, FastLaneWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, FastLaneWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
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
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleUpdates(context)
        fetchPriceAndUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopScheduledUpdates()
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
                updateWidgetWithPrice(context, price)
                
                // Schedule next update if in active hours
                if (isActiveHours()) {
                    scheduleNextUpdate(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateWidgetWithError(context)
            }
        }.start()
    }

    private fun showLoadingState(context: Context) {
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
            
            // Show loading indicator
            views.setViewVisibility(R.id.loading_progress, android.view.View.VISIBLE)
            
            // Update time text to show "מעדכן..." for large layout
            if (layoutId == R.layout.widget_layout) {
                views.setTextViewText(R.id.update_time, "מעדכן...")
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun updateWidgetWithPrice(context: Context, price: Int) {
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
            
            // Hide loading indicator
            views.setViewVisibility(R.id.loading_progress, android.view.View.GONE)
            
            // Update price
            views.setTextViewText(R.id.price_text, price.toString())
            
            // Get current theme colors
            val themeName = WidgetPreferences.getColorTheme(context)
            val colors = when (themeName) {
                "vibrant" -> ThemeColors("#4CAF50", "#66BB6A", "#FFC107", "#FFD54F", "#F44336", "#EF5350")
                "dark" -> ThemeColors("#2E7D32", "#388E3C", "#F57C00", "#FB8C00", "#C62828", "#D32F2F")
                "minimal" -> ThemeColors("#E8F5E9", "#F1F8F4", "#FFF8E1", "#FFFDE7", "#FFEBEE", "#FFCDD2")
                "neon" -> ThemeColors("#00FF88", "#00FFAA", "#FFFF00", "#FFFF66", "#FF00FF", "#FF66FF")
                else -> ThemeColors("#A8E6CF", "#C1F0D5", "#FFE5B4", "#FFF4D6", "#FFB3BA", "#FFCCD1") // pastel default
            }
            
            // Update background color based on price and thresholds
            val threshold1 = WidgetPreferences.getLowToMediumThreshold(context)
            val threshold2 = WidgetPreferences.getMediumToHighThreshold(context)
            
            // Use start color as main background
            val backgroundColor = when {
                price <= threshold1 -> android.graphics.Color.parseColor(colors.greenStart)
                price <= threshold2 -> android.graphics.Color.parseColor(colors.yellowStart)
                else -> android.graphics.Color.parseColor(colors.redStart)
            }
            
            // Set solid background color with rounded corners
            views.setInt(R.id.widget_container, "setBackgroundColor", backgroundColor)
            
            // Update time (only for large layout)
            if (layoutId == R.layout.widget_layout) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale("he", "IL"))
                val currentTime = timeFormat.format(Date())
                views.setTextViewText(R.id.update_time, currentTime)
            }
            
            // Set click listener
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
            
            // Hide loading indicator
            views.setViewVisibility(R.id.loading_progress, android.view.View.GONE)
            
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

    private fun isActiveHours(): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"))
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Sunday (1) to Thursday (5) in Calendar, 07:00 to 12:00
        val isSundayToThursday = dayOfWeek >= Calendar.SUNDAY && dayOfWeek <= Calendar.THURSDAY
        val isActiveTime = hourOfDay in 7..11
        
        return isSundayToThursday && isActiveTime
    }

    private fun scheduleUpdates(context: Context) {
        scheduleNextUpdate(context)
    }

    private fun scheduleNextUpdate(context: Context) {
        stopScheduledUpdates()
        
        if (isActiveHours()) {
            updateRunnable = Runnable {
                fetchPriceAndUpdate(context)
            }
            handler.postDelayed(updateRunnable!!, 20000) // 20 seconds
        }
    }

    private fun stopScheduledUpdates() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }
}
