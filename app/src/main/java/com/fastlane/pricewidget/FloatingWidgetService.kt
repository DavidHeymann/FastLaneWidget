package com.fastlane.pricewidget

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
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

class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var priceText: TextView? = null
    private var shekelText: TextView? = null
    private var progressBar: ProgressBar? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    
    // For drawer mode
    private var isDrawerMode = false
    private var isDrawerExpanded = false
    private var screenWidth = 0
    private lateinit var params: WindowManager.LayoutParams
    
    // Long press detection
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var isLongPress = false
    private val LONG_PRESS_TIMEOUT = 1000L
    
    // Auto-collapse timer
    private val autoCollapseHandler = Handler(Looper.getMainLooper())
    private val AUTO_COLLAPSE_DELAY = 3000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        // Get screen width for drawer mode
        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = android.graphics.Point()
        display.getRealSize(size)
        screenWidth = size.x
        
        val widgetSize = WidgetPreferences.getFloatingSize(this)
        val layoutId = when (widgetSize) {
            "small" -> R.layout.floating_widget_small
            "large" -> R.layout.floating_widget_large
            else -> R.layout.floating_widget_medium
        }
        
        floatingView = LayoutInflater.from(this).inflate(layoutId, null)
        priceText = floatingView?.findViewById(R.id.floating_price)
        shekelText = floatingView?.findViewById(R.id.floating_shekel)
        progressBar = floatingView?.findViewById(R.id.floating_progress)
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        // Check if drawer mode is enabled
        isDrawerMode = WidgetPreferences.isDrawerMode(this)
        
        if (isDrawerMode) {
            // Start as collapsed drawer on the right edge (hidden), centered vertically
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
            params.y = 0  // CENTER_VERTICAL will handle this
            
            // Wait for view to be measured
            floatingView?.post {
                val viewWidth = floatingView?.width ?: 0
                // Hide completely, show only 15px tab
                params.x = -(viewWidth - 15)
                windowManager?.updateViewLayout(floatingView, params)
            }
            params.x = -200 // Initial guess
        } else {
            // Regular floating mode - start at top-left
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 100
        }
        
        // Apply opacity
        val opacity = WidgetPreferences.getFloatingOpacity(this)
        floatingView?.alpha = opacity
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(floatingView, params)
        
        setupTouchListener()
        
        // Update price
        refreshPrice()
    }
    
    private fun setupTouchListener() {
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        isLongPress = false
                        
                        // Cancel auto-collapse
                        autoCollapseHandler.removeCallbacksAndMessages(null)
                        
                        // Start long press detection
                        longPressHandler.postDelayed({
                            isLongPress = true
                            onLongPress()
                        }, LONG_PRESS_TIMEOUT)
                        
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true
                            longPressHandler.removeCallbacksAndMessages(null)
                            
                            if (isDrawerMode) {
                                // In drawer mode - allow free dragging
                                if (!isDrawerExpanded) {
                                    // Expand first if collapsed
                                    expandDrawer()
                                }
                                
                                // Free drag
                                params.gravity = Gravity.TOP or Gravity.START
                                params.x = (event.rawX - floatingView!!.width / 2).toInt()
                                params.y = (event.rawY - floatingView!!.height / 2).toInt()
                                windowManager?.updateViewLayout(floatingView, params)
                            } else {
                                // Regular mode - free dragging
                                params.x = initialX + deltaX.toInt()
                                params.y = initialY + deltaY.toInt()
                                windowManager?.updateViewLayout(floatingView, params)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        longPressHandler.removeCallbacksAndMessages(null)
                        
                        if (isLongPress) {
                            return true
                        }
                        
                        if (!isDragging) {
                            // Click
                            if (isDrawerMode) {
                                toggleDrawer()
                            } else {
                                // Refresh price
                                refreshPrice()
                            }
                        } else {
                            // After dragging
                            if (isDrawerMode) {
                                // Smart snap to nearest edge
                                snapToNearestEdge(event.rawX, event.rawY)
                            }
                            // In regular mode, stay where dropped
                        }
                        
                        isDragging = false
                        return true
                    }
                }
                return false
            }
        })
    }
    
    private fun snapToNearestEdge(x: Float, y: Float) {
        // Determine which edge is closer
        val isLeftCloser = x < screenWidth / 2
        
        // Save the Y position (height)
        val centerY = y.toInt()
        
        // Snap to the nearest edge
        if (isLeftCloser) {
            // Snap to left edge
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        } else {
            // Snap to right edge
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        
        // Set Y position to maintain height
        params.y = centerY - (floatingView?.height ?: 0) / 2
        
        // Collapse to show only tab
        val viewWidth = floatingView?.width ?: 0
        params.x = -(viewWidth - 15)
        
        windowManager?.updateViewLayout(floatingView, params)
        isDrawerExpanded = false
    }
    
    private fun onLongPress() {
        // Show confirmation dialog via toast
        Toast.makeText(this, "לחיצה ארוכה נוספת תכבה את הWidget הצף", Toast.LENGTH_SHORT).show()
        
        // Schedule close after another second
        longPressHandler.postDelayed({
            // Disable floating widget
            WidgetPreferences.setFloatingWidgetEnabled(this, false)
            Toast.makeText(this, "Widget צף כובה", Toast.LENGTH_SHORT).show()
            stopSelf()
        }, 1000)
    }
    
    private fun toggleDrawer() {
        if (isDrawerExpanded) {
            collapseDrawer()
        } else {
            expandDrawer()
            // Refresh price when opening
            refreshPrice()
        }
    }
    
    private fun expandDrawer() {
        isDrawerExpanded = true
        
        // Animate to fully visible
        params.x = 0
        windowManager?.updateViewLayout(floatingView, params)
        
        // Schedule auto-collapse
        autoCollapseHandler.postDelayed({
            collapseDrawer()
        }, AUTO_COLLAPSE_DELAY)
    }
    
    private fun collapseDrawer() {
        isDrawerExpanded = false
        autoCollapseHandler.removeCallbacksAndMessages(null)
        
        // Animate to hidden (show only 15px tab)
        val viewWidth = floatingView?.width ?: 0
        params.x = -(viewWidth - 15)
        windowManager?.updateViewLayout(floatingView, params)
    }
    
    private fun refreshPrice() {
        // Show progress
        progressBar?.visibility = View.VISIBLE
        
        Thread {
            try {
                val price = PriceApi.getCurrentPrice()
                
                // Update UI on main thread
                Handler(Looper.getMainLooper()).post {
                    // Hide progress
                    progressBar?.visibility = View.GONE
                    
                    priceText?.text = price.toString()
                    
                    // Update color based on threshold and theme
                    val threshold1 = WidgetPreferences.getLowToMediumThreshold(this)
                    val threshold2 = WidgetPreferences.getMediumToHighThreshold(this)
                    
                    // Get current theme
                    val themeName = WidgetPreferences.getColorTheme(this)
                    val theme = ColorTheme.values().find { it.id == themeName } ?: ColorTheme.PASTEL
                    val colors = theme.getColors()
                    
                    val color = when {
                        price <= threshold1 -> android.graphics.Color.parseColor(colors.greenStart)
                        price <= threshold2 -> android.graphics.Color.parseColor(colors.yellowStart)
                        else -> android.graphics.Color.parseColor(colors.redStart)
                    }
                    
                    priceText?.setTextColor(color)
                    shekelText?.setTextColor(color)
                    
                    // Check for notifications
                    PriceNotificationManager.checkAndNotify(this, price)
                    
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

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
        longPressHandler.removeCallbacksAndMessages(null)
        autoCollapseHandler.removeCallbacksAndMessages(null)
    }
}
