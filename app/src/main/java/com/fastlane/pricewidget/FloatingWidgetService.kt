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
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

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
    
    // Long press detection
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var isLongPress = false
    private val LONG_PRESS_TIMEOUT = 1000L

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
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        // Check if drawer mode is enabled
        isDrawerMode = WidgetPreferences.isDrawerMode(this)
        
        if (isDrawerMode) {
            // Start as collapsed drawer on the right edge
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
            params.x = -(floatingView?.width ?: 100) + 20 // Show only 20px
        } else {
            // Regular floating mode
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 100
        }
        
        // Apply opacity
        val opacity = WidgetPreferences.getFloatingOpacity(this)
        floatingView?.alpha = opacity
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(floatingView, params)
        
        setupTouchListener(params)
        
        // Update price
        refreshPrice()
    }
    
    private fun setupTouchListener(params: WindowManager.LayoutParams) {
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
                        
                        // Start long press detection
                        longPressHandler.postDelayed({
                            isLongPress = true
                            onLongPress()
                        }, LONG_PRESS_TIMEOUT)
                        
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = Math.abs(event.rawX - initialTouchX)
                        val deltaY = Math.abs(event.rawY - initialTouchY)
                        
                        if (deltaX > 10 || deltaY > 10) {
                            isDragging = true
                            longPressHandler.removeCallbacksAndMessages(null)
                            
                            if (!isDrawerMode) {
                                // Regular dragging
                                params.x = initialX + (event.rawX - initialTouchX).toInt()
                                params.y = initialY + (event.rawY - initialTouchY).toInt()
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
                            // Click - toggle drawer or refresh
                            if (isDrawerMode) {
                                toggleDrawer(params)
                            } else {
                                refreshPrice()
                            }
                        } else if (isDrawerMode) {
                            // Snap back to edge after drag
                            snapToEdge(params)
                        }
                        
                        isDragging = false
                        return true
                    }
                }
                return false
            }
        })
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
    
    private fun toggleDrawer(params: WindowManager.LayoutParams) {
        isDrawerExpanded = !isDrawerExpanded
        
        if (isDrawerExpanded) {
            // Expand - show full widget
            params.x = 0
        } else {
            // Collapse - show only edge
            params.x = -(floatingView?.width ?: 100) + 20
        }
        
        windowManager?.updateViewLayout(floatingView, params)
    }
    
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        // Snap to nearest edge (left or right)
        val screenCenter = screenWidth / 2
        
        if (params.x < screenCenter) {
            // Snap to left
            params.x = -(floatingView?.width ?: 100) + 20
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        } else {
            // Snap to right
            params.x = -(floatingView?.width ?: 100) + 20
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        
        windowManager?.updateViewLayout(floatingView, params)
    }
    
    private fun refreshPrice() {
        // Show progress
        progressBar?.visibility = View.VISIBLE
        
        PriceApi.fetchPrice(this) { price ->
            // Hide progress
            progressBar?.visibility = View.GONE
            
            if (price != null) {
                priceText?.text = price.toString()
                
                // Update color based on threshold
                val threshold1 = WidgetPreferences.getLowToMediumThreshold(this)
                val threshold2 = WidgetPreferences.getMediumToHighThreshold(this)
                
                val color = when {
                    price <= threshold1 -> android.graphics.Color.parseColor("#27AE60")
                    price <= threshold2 -> android.graphics.Color.parseColor("#F39C12")
                    else -> android.graphics.Color.parseColor("#E74C3C")
                }
                
                priceText?.setTextColor(color)
                shekelText?.setTextColor(color)
                
                // Check for notifications
                PriceNotificationManager.checkAndNotify(this, price)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
        longPressHandler.removeCallbacksAndMessages(null)
    }
}
