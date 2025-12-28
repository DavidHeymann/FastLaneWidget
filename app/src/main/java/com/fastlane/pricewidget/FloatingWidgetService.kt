package com.fastlane.pricewidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var floatingContainer: View? = null
    private var closeTarget: View? = null
    private var priceText: TextView? = null
    private var shekelText: TextView? = null
    private var progressBar: ProgressBar? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var isLongPressing = false
    
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val LONG_PRESS_TIMEOUT = 500L
    
    private var closeTargetParams: WindowManager.LayoutParams? = null
    private var closeTargetSize = 0 // Will be calculated from dp
    
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "floating_widget_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        // Start as foreground service to prevent being killed
        startForeground(NOTIFICATION_ID, createNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Get widget size preference
        val size = WidgetPreferences.getFloatingSize(this)
        val layoutId = when (size) {
            "small" -> R.layout.floating_widget_small
            "large" -> R.layout.floating_widget_large
            else -> R.layout.floating_widget_medium
        }
        
        floatingView = LayoutInflater.from(this).inflate(layoutId, null)

        floatingContainer = floatingView?.findViewById(R.id.floating_container)
        priceText = floatingView?.findViewById(R.id.floating_price)
        shekelText = floatingView?.findViewById(R.id.floating_shekel)
        progressBar = floatingView?.findViewById(R.id.floating_progress)
        
        // Get saved position or use default
        val savedX = WidgetPreferences.getFloatingX(this)
        val savedY = WidgetPreferences.getFloatingY(this)
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = savedX
        params.y = savedY
        
        // Get opacity preference
        val opacity = WidgetPreferences.getFloatingOpacity(this)
        floatingView?.alpha = opacity
        
        windowManager?.addView(floatingView, params)
        
        setupCloseTarget()
        setupTouchListener()
        
        // Load last saved price if available
        val lastPrice = WidgetPreferences.getLastPrice(this)
        if (lastPrice > 0) {
            updatePriceDisplay(lastPrice)
        }
        
        // Initial price fetch
        refreshPrice()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY to restart service if killed by system
        return START_STICKY
    }

    private fun setupCloseTarget() {
        // Calculate close target size in pixels (100dp)
        val density = resources.displayMetrics.density
        closeTargetSize = (100 * density).toInt()

        // Create close target (transparent circle with white border and white X)
        closeTarget = LayoutInflater.from(this).inflate(R.layout.close_target, null)
        
        closeTargetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100 // 100px from bottom
        }
        
        // Don't add to window yet - only show when dragging
    }
    
    private fun setupTouchListener() {
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                event ?: return false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        isLongPressing = false
                        
                        // Start long press timer
                        longPressHandler.postDelayed({
                            isLongPressing = true
                            showCloseTarget()
                        }, LONG_PRESS_TIMEOUT)
                        
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            if (!isDragging) {
                                isDragging = true
                                // Cancel long press if started dragging
                                if (!isLongPressing) {
                                    longPressHandler.removeCallbacksAndMessages(null)
                                }
                            }
                            
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()
                            windowManager?.updateViewLayout(floatingView, params)
                            
                            // Update close target appearance if long pressing
                            if (isLongPressing) {
                                updateCloseTargetProximity(event.rawX, event.rawY)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        longPressHandler.removeCallbacksAndMessages(null)
                        
                        if (isLongPressing) {
                            hideCloseTarget()
                            
                            // Check if dropped on close target
                            if (isOverCloseTarget(event.rawX, event.rawY)) {
                                // Close the widget
                                WidgetPreferences.setFloatingWidgetEnabled(this@FloatingWidgetService, false)
                                Toast.makeText(this@FloatingWidgetService, "Widget צף כובה", Toast.LENGTH_SHORT).show()
                                stopSelf()
                                return true
                            }
                        }
                        
                        if (isDragging) {
                            // Save position after dragging
                            WidgetPreferences.setFloatingPosition(this@FloatingWidgetService, params.x, params.y)
                        } else if (!isLongPressing) {
                            // Click - refresh price
                            refreshPrice()
                        }
                        
                        isDragging = false
                        isLongPressing = false
                        return true
                    }
                }
                return false
            }
        })
    }
    
    private fun showCloseTarget() {
        try {
            closeTarget?.let {
                if (it.parent == null) {
                    windowManager?.addView(it, closeTargetParams)
                }
                it.alpha = 0f
                it.animate()
                    .alpha(1f)
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(200)
                    .start()
            }
        } catch (e: Exception) {
            // View might already be added
        }
    }
    
    private fun hideCloseTarget() {
        closeTarget?.let {
            it.animate()
                .alpha(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .withEndAction {
                    try {
                        windowManager?.removeView(it)
                    } catch (e: Exception) {
                        // View might already be removed
                    }
                }
                .start()
        }
    }
    
    private fun updateCloseTargetProximity(x: Float, y: Float) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Close target is at bottom center
        val targetX = screenWidth / 2f
        val targetY = screenHeight - 100f - closeTargetSize / 2f

        val distance = Math.sqrt(
            Math.pow((x - targetX).toDouble(), 2.0) +
            Math.pow((y - targetY).toDouble(), 2.0)
        ).toFloat()

        // Detection area matches the circle radius exactly
        val detectionRadius = closeTargetSize / 2f

        // Visual feedback: scale up when widget is inside the circle
        if (distance < detectionRadius) {
            // Widget is in close zone - scale up
            closeTarget?.animate()
                ?.scaleX(1.3f)
                ?.scaleY(1.3f)
                ?.alpha(1f)
                ?.setDuration(150)
                ?.start()
        } else {
            // Widget is outside - normal size
            closeTarget?.animate()
                ?.scaleX(1.0f)
                ?.scaleY(1.0f)
                ?.alpha(0.8f)
                ?.setDuration(150)
                ?.start()
        }
    }

    private fun isOverCloseTarget(x: Float, y: Float): Boolean {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Close target is at bottom center
        val targetX = screenWidth / 2f
        val targetY = screenHeight - 100f - closeTargetSize / 2f

        val distance = Math.sqrt(
            Math.pow((x - targetX).toDouble(), 2.0) +
            Math.pow((y - targetY).toDouble(), 2.0)
        ).toFloat()

        // Widget closes when dropped inside the circle
        return distance < closeTargetSize / 2f
    }
    
    private fun refreshPrice() {
        // Show progress
        progressBar?.visibility = View.VISIBLE
        
        Thread {
            try {
                val price = PriceApi.getCurrentPrice()
                
                Handler(Looper.getMainLooper()).post {
                    // Hide progress
                    progressBar?.visibility = View.GONE
                    
                    // Update this floating widget directly
                    updatePriceDisplay(price)
                    
                    // Also broadcast to update home widgets and save price
                    PriceUpdateReceiver.broadcastPriceUpdate(this, price)
                    
                    // Show feedback toast
                    Toast.makeText(this, "עודכן: ₪$price", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Hide progress on error
                Handler(Looper.getMainLooper()).post {
                    progressBar?.visibility = View.GONE
                    Toast.makeText(this, "שגיאה בעדכון מחיר", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun updatePriceDisplay(price: Int) {
        priceText?.text = price.toString()

        // Update background color based on threshold and theme
        val threshold1 = WidgetPreferences.getLowToMediumThreshold(this)
        val threshold2 = WidgetPreferences.getMediumToHighThreshold(this)

        // Get current theme
        val themeName = WidgetPreferences.getColorTheme(this)

        // Determine background color based on price zone and theme
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

        // Update background color while preserving rounded corners
        val backgroundColor = android.graphics.Color.parseColor(colorHex)

        // Create a rounded rectangle drawable with the new color
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f * resources.displayMetrics.density // 24dp radius
            setColor(backgroundColor)
            setStroke(
                (2 * resources.displayMetrics.density).toInt(),
                android.graphics.Color.parseColor("#E0E0E0")
            )
        }
        floatingContainer?.background = drawable

        // Set text color to black for maximum contrast
        val textColor = android.graphics.Color.BLACK
        priceText?.setTextColor(textColor)
        shekelText?.setTextColor(textColor)

        // Check for notifications
        PriceNotificationManager.checkAndNotify(this, price)
    }

    private fun createNotification(): Notification {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Floating Widget Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating price widget running"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        // Create intent to open main activity when notification is clicked
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Fast Lane Widget")
            .setContentText("Widget צף פעיל")
            .setSmallIcon(R.drawable.ic_road)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Remove handlers
        longPressHandler.removeCallbacksAndMessages(null)
        
        // Remove close target
        try {
            closeTarget?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            // Already removed
        }
        
        // Remove floating view
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
