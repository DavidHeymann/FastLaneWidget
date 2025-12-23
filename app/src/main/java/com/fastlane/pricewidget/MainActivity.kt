package com.fastlane.pricewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var priceText: TextView
    private lateinit var refreshButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.status_text)
        priceText = findViewById(R.id.price_display)
        refreshButton = findViewById(R.id.refresh_button)
        
        refreshButton.setOnClickListener {
            refreshPrice()
        }
        
        checkWidgetStatus()
    }
    
    private fun checkWidgetStatus() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(this, FastLaneWidget::class.java)
        )
        
        if (widgetIds.isEmpty()) {
            statusText.text = "Widget לא מוסף למסך הבית\n\nלחץ לחיצה ארוכה על המסך הבית → Widgets → Nativ Mahir Price"
        } else {
            statusText.text = "Widget פעיל!\n${widgetIds.size} widget(s) על המסך"
            refreshPrice()
        }
    }
    
    private fun refreshPrice() {
        statusText.text = "מעדכן מחיר..."
        priceText.text = "--"
        refreshButton.isEnabled = false
        
        Thread {
            try {
                val price = PriceApi.getCurrentPrice()
                runOnUiThread {
                    priceText.text = "₪$price"
                    val colorRes = when {
                        price <= 10 -> R.color.price_green
                        price <= 25 -> R.color.price_yellow
                        else -> R.color.price_red
                    }
                    priceText.setTextColor(getColor(colorRes))
                    statusText.text = "המחיר הנוכחי בנתיב המהיר"
                    refreshButton.isEnabled = true
                    
                    // Update widgets
                    FastLaneWidget.updateAllWidgets(this)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "שגיאה: ${e.message}"
                    priceText.text = "!"
                    refreshButton.isEnabled = true
                }
            }
        }.start()
    }
}
