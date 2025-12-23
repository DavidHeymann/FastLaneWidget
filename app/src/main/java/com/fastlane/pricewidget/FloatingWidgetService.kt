package com.fastlane.pricewidget

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var priceText: TextView? = null
    private var shekelText: TextView? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        val size = WidgetPreferences.getFloatingSize(this)
        val layoutId = when (size) {
            "small" -> R.layout.floating_widget_small
            "large" -> R.layout.floating_widget_large
            else -> R.layout.floating_widget_medium
        }
        
        floatingView = LayoutInflater.from(this).inflate(layoutId, null)
        priceText = floatingView?.findViewById(R.id.floating_price)
        shekelText = floatingView?.findViewById(R.id.floating_shekel)
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        
        // Apply opacity
        val opacity = WidgetPreferences.getFloatingOpacity(this)
        floatingView?.alpha = opacity
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(floatingView, params)
        
        // Touch listener for dragging
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = Math.abs(event.rawX - initialTouchX)
                        val deltaY = Math.abs(event.rawY - initialTouchY)
                        if (deltaX < 10 && deltaY < 10) {
                            // Click - refresh price
                            refreshPrice()
                        }
                        return true
                    }
                }
                return false
            }
        })
        
        // Initial price fetch
        refreshPrice()
        
        // Schedule periodic updates
        startPeriodicUpdates()
    }

    private fun refreshPrice() {
        Thread {
            try {
                val price = PriceApi.getCurrentPrice()
                post {
                    updatePrice(price)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun updatePrice(price: Int) {
        priceText?.text = price.toString()
        
        // Check if we should send notification
        PriceNotificationManager.checkAndNotify(this, price)
        
        // Update background color based on price
        val theme = WidgetPreferences.getColorTheme(this)
        val colors = theme.getColors()
        
        val colorStart = when {
            price <= 10 -> android.graphics.Color.parseColor(colors.greenStart)
            price <= 25 -> android.graphics.Color.parseColor(colors.yellowStart)
            else -> android.graphics.Color.parseColor(colors.redStart)
        }
        
        floatingView?.setBackgroundColor(colorStart)
    }

    private fun startPeriodicUpdates() {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (shouldUpdate()) {
                    refreshPrice()
                }
            }
        }, 0, getUpdateInterval())
    }

    private fun shouldUpdate(): Boolean {
        if (!WidgetPreferences.isAutoUpdateEnabled(this)) {
            return false
        }
        
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"))
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        
        val activeDays = WidgetPreferences.getActiveDays(this)
        val startHour = WidgetPreferences.getUpdateStartHour(this)
        val endHour = WidgetPreferences.getUpdateEndHour(this)
        
        return activeDays.contains(dayOfWeek) && hourOfDay in startHour until endHour
    }

    private fun getUpdateInterval(): Long {
        return (WidgetPreferences.getUpdateInterval(this) * 1000).toLong()
    }

    private fun post(action: () -> Unit) {
        floatingView?.post(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
