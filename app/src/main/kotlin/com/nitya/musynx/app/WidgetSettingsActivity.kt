package com.nitya.musynx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nitya.musynx.app.ui.common.WidgetCustomizationScreen
import com.nitya.musynx.app.ui.styling.AppTheme // Adjust import if needed

class WidgetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme(darkTheme = true) {
                WidgetCustomizationScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}