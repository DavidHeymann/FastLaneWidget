package com.fastlane.pricewidget

import android.content.Context
import android.content.SharedPreferences

object WidgetPreferences {
    private const val PREFS_NAME = "widget_prefs"
    
    // Keys
    private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
    private const val KEY_UPDATE_START_HOUR = "update_start_hour"
    private const val KEY_UPDATE_END_HOUR = "update_end_hour"
    private const val KEY_UPDATE_INTERVAL = "update_interval_seconds"
    private const val KEY_ACTIVE_DAYS = "active_days"
    private const val KEY_COLOR_THEME = "color_theme"
    private const val KEY_FLOATING_WIDGET_ENABLED = "floating_widget_enabled"
    private const val KEY_FLOATING_SIZE = "floating_size"
    private const val KEY_FLOATING_OPACITY = "floating_opacity"
    private const val KEY_PRICE_ALERT_ENABLED = "price_alert_enabled"
    private const val KEY_THRESHOLD_LOW_TO_MEDIUM = "threshold_low_to_medium"
    private const val KEY_THRESHOLD_MEDIUM_TO_HIGH = "threshold_medium_to_high"
    
    // Default values
    const val DEFAULT_START_HOUR = 7
    const val DEFAULT_END_HOUR = 12
    const val DEFAULT_INTERVAL = 20 // seconds
    const val DEFAULT_DAYS = "1,2,3,4,5" // Sunday-Thursday
    const val DEFAULT_THEME = "pastel"
    const val DEFAULT_THRESHOLD_LOW_TO_MEDIUM = 10  // Green to Yellow
    const val DEFAULT_THRESHOLD_MEDIUM_TO_HIGH = 25  // Yellow to Red
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Auto update settings
    fun isAutoUpdateEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_UPDATE_ENABLED, true)
    }
    
    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled).apply()
    }
    
    fun getUpdateStartHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_UPDATE_START_HOUR, DEFAULT_START_HOUR)
    }
    
    fun setUpdateStartHour(context: Context, hour: Int) {
        getPrefs(context).edit().putInt(KEY_UPDATE_START_HOUR, hour).apply()
    }
    
    fun getUpdateEndHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_UPDATE_END_HOUR, DEFAULT_END_HOUR)
    }
    
    fun setUpdateEndHour(context: Context, hour: Int) {
        getPrefs(context).edit().putInt(KEY_UPDATE_END_HOUR, hour).apply()
    }
    
    fun getUpdateInterval(context: Context): Int {
        return getPrefs(context).getInt(KEY_UPDATE_INTERVAL, DEFAULT_INTERVAL)
    }
    
    fun setUpdateInterval(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_UPDATE_INTERVAL, seconds).apply()
    }
    
    // Active days (comma-separated: 1=Sunday, 2=Monday, etc.)
    fun getActiveDays(context: Context): Set<Int> {
        val daysString = getPrefs(context).getString(KEY_ACTIVE_DAYS, DEFAULT_DAYS) ?: DEFAULT_DAYS
        return daysString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
    
    fun setActiveDays(context: Context, days: Set<Int>) {
        val daysString = days.sorted().joinToString(",")
        getPrefs(context).edit().putString(KEY_ACTIVE_DAYS, daysString).apply()
    }
    
    // Color theme
    fun getColorTheme(context: Context): String {
        return getPrefs(context).getString(KEY_COLOR_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }
    
    fun setColorTheme(context: Context, themeId: String) {
        getPrefs(context).edit().putString(KEY_COLOR_THEME, themeId).apply()
    }
    
    // Floating widget
    fun isFloatingWidgetEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FLOATING_WIDGET_ENABLED, false)
    }
    
    fun setFloatingWidgetEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FLOATING_WIDGET_ENABLED, enabled).apply()
    }
    
    fun getFloatingSize(context: Context): String {
        return getPrefs(context).getString(KEY_FLOATING_SIZE, "medium") ?: "medium"
    }
    
    fun setFloatingSize(context: Context, size: String) {
        getPrefs(context).edit().putString(KEY_FLOATING_SIZE, size).apply()
    }
    
    fun getFloatingOpacity(context: Context): Float {
        return getPrefs(context).getFloat(KEY_FLOATING_OPACITY, 0.9f)
    }
    
    fun setFloatingOpacity(context: Context, opacity: Float) {
        getPrefs(context).edit().putFloat(KEY_FLOATING_OPACITY, opacity).apply()
    }
    
    // Drawer mode
    fun isDrawerMode(context: Context): Boolean {
        return getPrefs(context).getBoolean("drawer_mode", false)
    }
    
    fun setDrawerMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("drawer_mode", enabled).apply()
    }
    
    // Price alerts
    fun isPriceAlertEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PRICE_ALERT_ENABLED, false)
    }
    
    fun setPriceAlertEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PRICE_ALERT_ENABLED, enabled).apply()
    }
    
    // Threshold 1: Green to Yellow (default 10)
    fun getLowToMediumThreshold(context: Context): Int {
        return getPrefs(context).getInt(KEY_THRESHOLD_LOW_TO_MEDIUM, DEFAULT_THRESHOLD_LOW_TO_MEDIUM)
    }
    
    fun setLowToMediumThreshold(context: Context, threshold: Int) {
        getPrefs(context).edit().putInt(KEY_THRESHOLD_LOW_TO_MEDIUM, threshold).apply()
    }
    
    // Threshold 2: Yellow to Red (default 25)
    fun getMediumToHighThreshold(context: Context): Int {
        return getPrefs(context).getInt(KEY_THRESHOLD_MEDIUM_TO_HIGH, DEFAULT_THRESHOLD_MEDIUM_TO_HIGH)
    }
    
    fun setMediumToHighThreshold(context: Context, threshold: Int) {
        getPrefs(context).edit().putInt(KEY_THRESHOLD_MEDIUM_TO_HIGH, threshold).apply()
    }
    
    // Get color zone for a price
    fun getPriceZone(context: Context, price: Int): PriceZone {
        val lowToMedium = getLowToMediumThreshold(context)
        val mediumToHigh = getMediumToHighThreshold(context)
        
        return when {
            price <= lowToMedium -> PriceZone.GREEN
            price <= mediumToHigh -> PriceZone.YELLOW
            else -> PriceZone.RED
        }
    }
}

enum class PriceZone {
    GREEN,   // ≤ threshold1
    YELLOW,  // threshold1 < price ≤ threshold2
    RED      // > threshold2
}
