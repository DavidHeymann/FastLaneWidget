package com.fastlane.pricewidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver that listens for price update broadcasts and updates all widgets
 */
class PriceUpdateReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_PRICE_UPDATED = "com.fastlane.pricewidget.PRICE_UPDATED"
        const val EXTRA_PRICE = "extra_price"
        
        /**
         * Broadcast price update to all widgets and components
         */
        fun broadcastPriceUpdate(context: Context, price: Int) {
            val intent = Intent(ACTION_PRICE_UPDATED).apply {
                putExtra(EXTRA_PRICE, price)
            }
            context.sendBroadcast(intent)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PRICE_UPDATED) {
            val price = intent.getIntExtra(EXTRA_PRICE, -1)
            if (price > 0) {
                // Save price to preferences - widgets update themselves directly
                WidgetPreferences.setLastPrice(context, price)
                WidgetPreferences.setLastUpdateTime(context, System.currentTimeMillis())
            }
        }
    }
}
