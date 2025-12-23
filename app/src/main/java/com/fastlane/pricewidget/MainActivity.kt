package com.fastlane.pricewidget

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var autoUpdateSwitch: Switch
    private lateinit var startHourPicker: NumberPicker
    private lateinit var endHourPicker: NumberPicker
    private lateinit var intervalSeekBar: SeekBar
    private lateinit var intervalText: TextView
    private lateinit var daysContainer: LinearLayout
    private lateinit var colorThemeSpinner: Spinner
    private lateinit var floatingWidgetSwitch: Switch
    private lateinit var floatingSizeSpinner: Spinner
    private lateinit var floatingOpacitySeekBar: SeekBar
    private lateinit var drawerModeSwitch: Switch
    private lateinit var priceAlertSwitch: Switch
    private lateinit var threshold1SeekBar: SeekBar
    private lateinit var threshold1Text: TextView
    private lateinit var threshold2SeekBar: SeekBar
    private lateinit var threshold2Text: TextView
    private lateinit var resetAlertsButton: Button
    private lateinit var saveButton: Button
    private lateinit var currentPriceText: TextView
    private lateinit var refreshButton: Button

    private val dayCheckBoxes = mutableMapOf<Int, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize notification channel
        PriceNotificationManager.createNotificationChannel(this)

        // Request notification permission (Android 13+)
        requestNotificationPermission()

        initViews()
        loadSettings()
        setupListeners()
        updateCurrentPrice()
    }
    
    override fun onResume() {
        super.onResume()
        // Sync floating widget switch with actual state
        floatingWidgetSwitch.isChecked = WidgetPreferences.isFloatingWidgetEnabled(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "הרשאת התראות ניתנה ✅", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "ללא הרשאת התראות, לא תקבל עדכונים", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initViews() {
        autoUpdateSwitch = findViewById(R.id.auto_update_switch)
        startHourPicker = findViewById(R.id.start_hour_picker)
        endHourPicker = findViewById(R.id.end_hour_picker)
        intervalSeekBar = findViewById(R.id.interval_seekbar)
        intervalText = findViewById(R.id.interval_text)
        daysContainer = findViewById(R.id.days_container)
        colorThemeSpinner = findViewById(R.id.color_theme_spinner)
        floatingWidgetSwitch = findViewById(R.id.floating_widget_switch)
        floatingSizeSpinner = findViewById(R.id.floating_size_spinner)
        floatingOpacitySeekBar = findViewById(R.id.floating_opacity_seekbar)
        drawerModeSwitch = findViewById(R.id.drawer_mode_switch)
        priceAlertSwitch = findViewById(R.id.price_alert_switch)
        threshold1SeekBar = findViewById(R.id.threshold1_seekbar)
        threshold1Text = findViewById(R.id.threshold1_text)
        threshold2SeekBar = findViewById(R.id.threshold2_seekbar)
        threshold2Text = findViewById(R.id.threshold2_text)
        resetAlertsButton = findViewById(R.id.reset_alerts_button)
        saveButton = findViewById(R.id.save_button)
        currentPriceText = findViewById(R.id.current_price_text)
        refreshButton = findViewById(R.id.refresh_button)

        // Setup hour pickers
        startHourPicker.minValue = 0
        startHourPicker.maxValue = 23
        endHourPicker.minValue = 0
        endHourPicker.maxValue = 23

        // Setup days checkboxes
        val days = listOf(
            1 to "א׳", 2 to "ב׳", 3 to "ג׳", 
            4 to "ד׳", 5 to "ה׳", 6 to "ו׳", 7 to "ש׳"
        )
        days.forEach { (dayNum, dayName) ->
            val checkBox = CheckBox(this).apply {
                text = dayName
                textSize = 16f
            }
            dayCheckBoxes[dayNum] = checkBox
            daysContainer.addView(checkBox)
        }

        // Setup color theme spinner
        val themes = ColorTheme.values().map { it.displayName }
        colorThemeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            themes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Setup floating size spinner
        val sizes = listOf("קטן", "בינוני", "גדול")
        floatingSizeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sizes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun loadSettings() {
        // Auto update
        autoUpdateSwitch.isChecked = WidgetPreferences.isAutoUpdateEnabled(this)
        startHourPicker.value = WidgetPreferences.getUpdateStartHour(this)
        endHourPicker.value = WidgetPreferences.getUpdateEndHour(this)
        
        val interval = WidgetPreferences.getUpdateInterval(this)
        intervalSeekBar.progress = interval
        intervalText.text = "$interval שניות"

        // Days
        val activeDays = WidgetPreferences.getActiveDays(this)
        dayCheckBoxes.forEach { (dayNum, checkBox) ->
            checkBox.isChecked = activeDays.contains(dayNum)
        }

        // Color theme
        val currentTheme = WidgetPreferences.getColorTheme(this)
        colorThemeSpinner.setSelection(ColorTheme.values().indexOf(currentTheme))

        // Floating widget
        floatingWidgetSwitch.isChecked = WidgetPreferences.isFloatingWidgetEnabled(this)
        val floatingSize = WidgetPreferences.getFloatingSize(this)
        floatingSizeSpinner.setSelection(
            when (floatingSize) {
                "small" -> 0
                "medium" -> 1
                "large" -> 2
                else -> 1
            }
        )
        floatingOpacitySeekBar.progress = (WidgetPreferences.getFloatingOpacity(this) * 100).toInt()
        
        // Drawer mode
        drawerModeSwitch.isChecked = WidgetPreferences.isDrawerMode(this)

        // Price alerts
        priceAlertSwitch.isChecked = WidgetPreferences.isPriceAlertEnabled(this)
        
        val threshold1 = WidgetPreferences.getLowToMediumThreshold(this)
        threshold1SeekBar.progress = threshold1
        threshold1Text.text = "₪$threshold1"
        
        val threshold2 = WidgetPreferences.getMediumToHighThreshold(this)
        threshold2SeekBar.progress = threshold2
        threshold2Text.text = "₪$threshold2"
    }

    private fun setupListeners() {
        // Auto update
        autoUpdateSwitch.setOnCheckedChangeListener { _, isChecked ->
            WidgetPreferences.setAutoUpdateEnabled(this, isChecked)
            updateWidgets()
        }

        startHourPicker.setOnValueChangedListener { _, _, newVal ->
            WidgetPreferences.setUpdateStartHour(this, newVal)
            updateWidgets()
        }

        endHourPicker.setOnValueChangedListener { _, _, newVal ->
            WidgetPreferences.setUpdateEndHour(this, newVal)
            updateWidgets()
        }

        intervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val seconds = Math.max(5, progress)
                intervalText.text = "$seconds שניות"
                if (fromUser) {
                    WidgetPreferences.setUpdateInterval(this@MainActivity, seconds)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateWidgets()
            }
        })

        // Days
        dayCheckBoxes.forEach { (dayNum, checkBox) ->
            checkBox.setOnCheckedChangeListener { _, _ ->
                val selectedDays = dayCheckBoxes.filter { it.value.isChecked }.keys
                WidgetPreferences.setActiveDays(this, selectedDays)
                updateWidgets()
            }
        }

        // Color theme
        colorThemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val theme = ColorTheme.values()[position]
                WidgetPreferences.setColorTheme(this@MainActivity, theme)
                updateWidgets()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Floating widget
        floatingWidgetSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !canDrawOverlays()) {
                requestOverlayPermission()
                floatingWidgetSwitch.isChecked = false
            } else {
                WidgetPreferences.setFloatingWidgetEnabled(this, isChecked)
                if (isChecked) {
                    startFloatingWidget()
                } else {
                    stopFloatingWidget()
                }
            }
        }

        floatingSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val size = when (position) {
                    0 -> "small"
                    1 -> "medium"
                    2 -> "large"
                    else -> "medium"
                }
                WidgetPreferences.setFloatingSize(this@MainActivity, size)
                if (WidgetPreferences.isFloatingWidgetEnabled(this@MainActivity)) {
                    restartFloatingWidget()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        floatingOpacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val opacity = progress / 100f
                    WidgetPreferences.setFloatingOpacity(this@MainActivity, opacity)
                    if (WidgetPreferences.isFloatingWidgetEnabled(this@MainActivity)) {
                        restartFloatingWidget()
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Drawer mode
        drawerModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            WidgetPreferences.setDrawerMode(this, isChecked)
            if (WidgetPreferences.isFloatingWidgetEnabled(this)) {
                restartFloatingWidget()
            }
        }

        // Price alerts
        priceAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            WidgetPreferences.setPriceAlertEnabled(this, isChecked)
        }

        threshold1SeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold1Text.text = "₪$progress"
                if (fromUser) {
                    WidgetPreferences.setLowToMediumThreshold(this@MainActivity, progress)
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        threshold2SeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold2Text.text = "₪$progress"
                if (fromUser) {
                    WidgetPreferences.setMediumToHighThreshold(this@MainActivity, progress)
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Reset alerts button
        resetAlertsButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("איפוס התראות")
                .setMessage("האם לאפס את מצב ההתראות? תקבל התראה חדשה בפעם הבאה שהמחיר יחצה את הסף.")
                .setPositiveButton("אפס") { _, _ ->
                    PriceNotificationManager.resetAlertState(this)
                    Toast.makeText(this, "ההתראות אופסו", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("ביטול", null)
                .show()
        }

        // Refresh button
        refreshButton.setOnClickListener {
            refreshButton.isEnabled = false
            refreshButton.text = "מעדכן..."
            Thread {
                try {
                    val price = PriceApi.getCurrentPrice()
                    runOnUiThread {
                        currentPriceText.text = "₪$price"
                        refreshButton.isEnabled = true
                        refreshButton.text = "רענן מחיר"
                        updateWidgets()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        currentPriceText.text = "שגיאה"
                        refreshButton.isEnabled = true
                        refreshButton.text = "רענן מחיר"
                        Toast.makeText(this, "שגיאה בקבלת מחיר", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
        
        // Save button - apply all changes
        saveButton.setOnClickListener {
            updateWidgets()
            if (WidgetPreferences.isFloatingWidgetEnabled(this)) {
                restartFloatingWidget()
            }
            Toast.makeText(this, "✅ השינויים נשמרו!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCurrentPrice() {
        Thread {
            try {
                val price = PriceApi.getCurrentPrice()
                runOnUiThread {
                    currentPriceText.text = "₪$price"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    currentPriceText.text = "לא זמין"
                }
            }
        }.start()
    }

    private fun updateWidgets() {
        val intent = Intent(this, FastLaneWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(this@MainActivity)
                .getAppWidgetIds(ComponentName(this@MainActivity, FastLaneWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show explanation toast
            Toast.makeText(
                this,
                "פותח הגדרות - אפשר הרשאה 'הצגה מעל אפליקציות אחרות'",
                Toast.LENGTH_LONG
            ).show()
            
            // Open settings directly
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            } catch (e: Exception) {
                Toast.makeText(this, "לא ניתן לפתוח הגדרות", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (canDrawOverlays()) {
                floatingWidgetSwitch.isChecked = true
                WidgetPreferences.setFloatingWidgetEnabled(this, true)
                startFloatingWidget()
                Toast.makeText(this, "Widget צף הופעל!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "הרשאה נדחתה", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFloatingWidget() {
        val intent = Intent(this, FloatingWidgetService::class.java)
        startService(intent)
    }

    private fun stopFloatingWidget() {
        val intent = Intent(this, FloatingWidgetService::class.java)
        stopService(intent)
    }

    private fun restartFloatingWidget() {
        stopFloatingWidget()
        Thread.sleep(100)
        startFloatingWidget()
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_NOTIFICATION_PERMISSION = 1002
    }
}
