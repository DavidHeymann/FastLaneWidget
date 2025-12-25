package com.fastlane.pricewidget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object PriceNotificationManager {
    
    private const val CHANNEL_ID = "price_alerts"
    private const val CHANNEL_NAME = "התראות מחיר"
    private const val NOTIFICATION_ID = 1001
    
    private const val PREF_LAST_ZONE = "last_price_zone"
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "התראות על שינוי אזור מחיר בנתיב המהיר"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Check if price crossed into a different zone and notify
     */
    fun checkAndNotify(context: Context, currentPrice: Int) {
        if (!WidgetPreferences.isPriceAlertEnabled(context)) {
            return
        }
        
        val currentZone = WidgetPreferences.getPriceZone(context, currentPrice)
        val prefs = context.getSharedPreferences("price_alerts", Context.MODE_PRIVATE)
        val lastZoneName = prefs.getString(PREF_LAST_ZONE, null)
        val lastZone = lastZoneName?.let { 
            try {
                PriceZone.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        
        // Check if we crossed into a different zone
        if (lastZone != null && lastZone != currentZone) {
            sendNotification(context, currentPrice, lastZone, currentZone)
        }
        
        // Save current zone
        prefs.edit().putString(PREF_LAST_ZONE, currentZone.name).apply()
    }
    
    private fun sendNotification(
        context: Context,
        price: Int,
        fromZone: PriceZone,
        toZone: PriceZone
    ) {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val threshold1 = WidgetPreferences.getLowToMediumThreshold(context)
        val threshold2 = WidgetPreferences.getMediumToHighThreshold(context)
        
        // Determine notification details based on zone transition
        val (emoji, title, color) = when (toZone) {
            PriceZone.GREEN -> Triple("🟢", "נתיב המהיר - תעריף רגיל", android.graphics.Color.parseColor("#27AE60"))
            PriceZone.YELLOW -> Triple("🟡", "נתיב המהיר - תעריף גבוה", android.graphics.Color.parseColor("#F39C12"))
            PriceZone.RED -> Triple("🔴", "נתיב המהיר - תעריף מוגזם", android.graphics.Color.parseColor("#E74C3C"))
        }
        
        val message = buildNotificationMessage(price, fromZone, toZone)
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$emoji $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(color)
            .setVibrate(longArrayOf(0, 500, 200, 500))
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun buildNotificationMessage(
        price: Int,
        fromZone: PriceZone,
        toZone: PriceZone
    ): String {
        return when {
            // To Green zone (regular price)
            toZone == PriceZone.GREEN -> "התעריף ירד לתעריף רגיל: ₪$price"
            
            // To Yellow from Green (price increased to high)
            fromZone == PriceZone.GREEN && toZone == PriceZone.YELLOW -> 
                "התעריף עלה לתעריף גבוה: ₪$price"
            
            // To Yellow from Red (price decreased to high)
            fromZone == PriceZone.RED && toZone == PriceZone.YELLOW -> 
                "התעריף ירד לתעריף גבוה: ₪$price"
            
            // To Red zone (excessive price)
            toZone == PriceZone.RED -> "התעריף קפץ לתעריף מוגזם: ₪$price"
            
            else -> "התעריף הנוכחי: ₪$price"
        }
    }
    
    fun resetAlertState(context: Context) {
        val prefs = context.getSharedPreferences("price_alerts", Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_LAST_ZONE).apply()
    }
}
