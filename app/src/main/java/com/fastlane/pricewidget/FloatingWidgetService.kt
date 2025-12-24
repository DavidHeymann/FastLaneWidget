package com.fastlane.pricewidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager

class FloatingWidgetService : Service(), FloatingViewListener {

    companion object {
        const val NOTIFICATION_ID = 100
    }

    private var floatingViewManager: FloatingViewManager? = null
    private var floatingView: View? = null
    
    // UI elements
    private var priceText: TextView? = null
    private var shekelText: TextView? = null
    private var progressBar: ProgressBar? = null
    
    // Drawer state tracking
    private var isDrawerMode = false
    private var isExpanded = false
    private var screenWidth = 0
    private var lastClickTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingViewManager != null) {
            return START_STICKY
        }

        // Get screen metrics
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels

        // Inflate the floating view
        val inflater = LayoutInflater.from(this)
        
        // Use appropriate size based on preferences
        val size = WidgetPreferences.getFloatingSize(this)
        val layoutRes = when (size) {
            "small" -> R.layout.floating_widget_small
            "large" -> R.layout.floating_widget_large
            else -> R.layout.floating_widget_medium
        }
        
        floatingView = inflater.inflate(layoutRes, null, false)
        
        // Get UI elements
        priceText = floatingView?.findViewById(R.id.price_text)
        shekelText = floatingView?.findViewById(R.id.shekel_text)
        progressBar = floatingView?.findViewById(R.id.progress_bar)
        
        // Check if drawer mode is enabled
        isDrawerMode = WidgetPreferences.isDrawerMode(this)
        
        // Setup click listener
        floatingView?.setOnClickListener {
            handleClick()
        }

        // Create FloatingViewManager
        floatingViewManager = FloatingViewManager(this, this)
        floatingViewManager?.apply {
            if (isDrawerMode) {
                // Drawer mode: snap to nearest edge
                setMoveDirection(FloatingViewManager.MOVE_DIRECTION_NEAREST)
            } else {
                // Regular mode: don't snap to edges
                setMoveDirection(FloatingViewManager.MOVE_DIRECTION_NONE)
            }
            
            // Enable physics animations for smooth movement
            setUsePhysicsBasedAnimation(true)
            
            // Always show (don't hide in fullscreen)
            setDisplayMode(FloatingViewManager.DISPLAY_MODE_SHOW_ALWAYS)
        }

        // Setup options
        val options = FloatingViewManager.Options().apply {
            // Margin from screen edge (in pixels)
            overMargin = (16 * metrics.density).toInt()
            
            // Check for saved position
            val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val savedX = prefs.getInt("last_x", -1)
            val savedY = prefs.getInt("last_y", -1)
            
            if (savedX != -1 && savedY != -1) {
                // Use saved position
                floatingViewX = savedX
                floatingViewY = savedY
            } else {
                // Default position: top-right
                floatingViewX = metrics.widthPixels - (100 * metrics.density).toInt()
                floatingViewY = (100 * metrics.density).toInt()
            }
        }

        // Add view to window
        floatingViewManager?.addViewToWindow(floatingView, options)

        // Apply opacity
        val opacity = WidgetPreferences.getFloatingOpacity(this)
        floatingView?.alpha = opacity

        // Start foreground
        startForeground(NOTIFICATION_ID, createNotification())

        // Initial price update
        refreshPrice()

        return START_STICKY
    }

    override fun onFinishFloatingView() {
        stopSelf()
    }

    override fun onTouchFinished(isFinishing: Boolean, x: Int, y: Int) {
        // Save position when touch finished
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("last_x", x)
            putInt("last_y", y)
            apply()
        }
        
        // Track if drawer is expanded or collapsed
        if (isDrawerMode) {
            // Calculate if widget is near edge (collapsed) or in center (expanded)
            val viewWidth = floatingView?.width ?: 0
            val distanceFromLeftEdge = x
            val distanceFromRightEdge = screenWidth - x - viewWidth
            
            // If less than 50px from edge, consider it collapsed
            isExpanded = distanceFromLeftEdge > 50 && distanceFromRightEdge > 50
        }
    }
    
    private fun handleClick() {
        // Prevent double clicks
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) {
            return
        }
        lastClickTime = currentTime
        
        if (isDrawerMode) {
            // In drawer mode: only refresh if already expanded
            if (isExpanded) {
                refreshPrice()
            }
            // If collapsed, the library will expand it automatically via drag
        } else {
            // In regular mode: always refresh
            refreshPrice()
        }
    }

    private fun refreshPrice() {
        progressBar?.visibility = View.VISIBLE
        priceText?.visibility = View.GONE
        shekelText?.visibility = View.GONE

        Thread {
            try {
                val price = PriceApi.getCurrentPrice()
                Handler(Looper.getMainLooper()).post {
                    progressBar?.visibility = View.GONE
                    priceText?.text = price.toString()
                    priceText?.visibility = View.VISIBLE
                    shekelText?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    progressBar?.visibility = View.GONE
                    priceText?.text = "--"
                    priceText?.visibility = View.VISIBLE
                    Toast.makeText(this, "שגיאה בטעינת המחיר", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun createNotification(): Notification {
        val channelId = "floating_widget_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Widget",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating widget on screen"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Fast Lane Price")
            .setContentText("Widget is active")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingViewManager?.removeAllViewToWindow()
    }
}
