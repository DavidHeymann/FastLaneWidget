package com.fastlane.pricewidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.torrydo.floatingbubbleview.ExpandableBubbleService
import com.torrydo.floatingbubbleview.service.expandable.BubbleBuilder
import com.torrydo.floatingbubbleview.service.expandable.ExpandedBubbleBuilder

class FloatingWidgetService : ExpandableBubbleService() {

    companion object {
        const val NOTIFICATION_ID = 100
        private const val CHANNEL_ID = "floating_widget_channel"
    }

    // UI elements for expanded view
    private var priceText: TextView? = null
    private var shekelText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var expandedView: View? = null

    // State tracking
    private var lastClickTime = 0L
    private var isDrawerMode = false

    override fun onCreate() {
        super.onCreate()
        
        // Check if drawer mode is enabled
        isDrawerMode = WidgetPreferences.isDrawerMode(this)
        
        // Start notification foreground
        startNotificationForeground()
        
        // Start in minimized state (small bubble)
        minimize()
    }

    override fun startNotificationForeground() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fast Lane Widget",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating price widget on screen"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        // Create notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fast Lane Price")
            .setContentText("Widget is active - tap to refresh")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun configBubble(): BubbleBuilder? {
        // Get widget size preference
        val size = WidgetPreferences.getFloatingSize(this)
        val layoutRes = when (size) {
            "small" -> R.layout.floating_widget_small
            "large" -> R.layout.floating_widget_large
            else -> R.layout.floating_widget_medium
        }

        // Inflate bubble view (collapsed state)
        val bubbleView = LayoutInflater.from(this).inflate(layoutRes, null, false)
        
        // Setup click listener for bubble
        bubbleView.setOnClickListener {
            handleBubbleClick()
        }

        // Get screen density for dp to px conversion
        val density = resources.displayMetrics.density
        
        // Load saved position
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val savedX = prefs.getInt("last_x", (16 * density).toInt())
        val savedY = prefs.getInt("last_y", (100 * density).toInt())

        return BubbleBuilder(this)
            // Set the bubble view
            .bubbleView(bubbleView)
            
            // Enable dragging
            .bubbleDraggable(true)
            
            // Start location
            .startLocationPx(savedX, savedY)
            
            // Enable animate to edge (for drawer mode)
            .enableAnimateToEdge(isDrawerMode)
            
            // Close bubble view (X button)
            .closeBubbleView(null) // We'll handle closing via settings
            
            // Apply opacity from settings
            .apply {
                val opacity = WidgetPreferences.getFloatingOpacity(this@FloatingWidgetService)
                bubbleView.alpha = opacity
            }
            
            // Bubble listeners
            .addFloatingBubbleListener(object : com.torrydo.floatingbubbleview.FloatingBubbleListener {
                override fun onFingerDown(x: Float, y: Float) {
                    // Touch started
                }

                override fun onFingerMove(x: Float, y: Float) {
                    // Being dragged
                }

                override fun onFingerUp(x: Float, y: Float) {
                    // Save position
                    savePosition(x.toInt(), y.toInt())
                }
            })
    }

    override fun configExpandedBubble(): ExpandedBubbleBuilder? {
        // Get widget size preference
        val size = WidgetPreferences.getFloatingSize(this)
        val layoutRes = when (size) {
            "small" -> R.layout.floating_widget_small
            "large" -> R.layout.floating_widget_large
            else -> R.layout.floating_widget_medium
        }

        // Inflate expanded view
        expandedView = LayoutInflater.from(this).inflate(layoutRes, null, false)
        
        // Get UI elements
        priceText = expandedView?.findViewById(R.id.price_text)
        shekelText = expandedView?.findViewById(R.id.shekel_text)
        progressBar = expandedView?.findViewById(R.id.progress_bar)
        
        // Setup click listener
        expandedView?.setOnClickListener {
            handleExpandedClick()
        }

        // Apply opacity
        val opacity = WidgetPreferences.getFloatingOpacity(this)
        expandedView?.alpha = opacity

        // Get screen density
        val density = resources.displayMetrics.density

        return ExpandedBubbleBuilder(this)
            // Set expanded view
            .expandedView(expandedView)
            
            // Start location (centered)
            .startLocationPx(
                (resources.displayMetrics.widthPixels / 2 - 100 * density).toInt(),
                (100 * density).toInt()
            )
            
            // Allow dragging when expanded
            .draggable(true)
            
            // Enable animate to edge
            .enableAnimateToEdge(isDrawerMode)
            
            // Dim background when expanded
            .dimAmount(0.3f)
            
            // Fill width based on size
            .fillMaxWidth(size == "large")
    }

    private fun handleBubbleClick() {
        // Prevent double clicks
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 300) {
            return
        }
        lastClickTime = currentTime

        if (isDrawerMode) {
            // In drawer mode: expand to show full widget
            expand()
            // Refresh price when expanded
            refreshPrice()
        } else {
            // In regular mode: just refresh price
            expand()
            refreshPrice()
        }
    }

    private fun handleExpandedClick() {
        // Prevent double clicks
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 300) {
            return
        }
        lastClickTime = currentTime

        if (isDrawerMode) {
            // In drawer mode: minimize back to bubble
            minimize()
        } else {
            // In regular mode: refresh price
            refreshPrice()
        }
    }

    private fun refreshPrice() {
        // Show loading
        progressBar?.visibility = View.VISIBLE
        priceText?.visibility = View.GONE
        shekelText?.visibility = View.GONE

        // Fetch price in background
        Thread {
            try {
                val price = PriceApi.getCurrentPrice()
                
                // Update UI on main thread
                Handler(Looper.getMainLooper()).post {
                    progressBar?.visibility = View.GONE
                    priceText?.text = price.toString()
                    priceText?.visibility = View.VISIBLE
                    shekelText?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                // Show error
                Handler(Looper.getMainLooper()).post {
                    progressBar?.visibility = View.GONE
                    priceText?.text = "--"
                    priceText?.visibility = View.VISIBLE
                    shekelText?.visibility = View.VISIBLE
                    Toast.makeText(
                        this@FloatingWidgetService,
                        "שגיאה בטעינת המחיר",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun savePosition(x: Int, y: Int) {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("last_x", x)
            putInt("last_y", y)
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup is handled by parent class
    }
}
