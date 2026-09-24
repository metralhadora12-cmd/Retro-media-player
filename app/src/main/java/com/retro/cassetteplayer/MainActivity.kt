package com.retro.cassetteplayer

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.retro.cassetteplayer.ui.RetroCassetteApp
import com.retro.cassetteplayer.ui.theme.AppTheme
import com.retro.cassetteplayer.ui.theme.RetroCassetteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppTheme.load(this)
        applySystemBars(AppTheme.isDark)
        setContent {
            // System-bar icons follow the app theme, not the device theme.
            val dark = AppTheme.isDark
            LaunchedEffect(dark) { applySystemBars(dark) }
            RetroCassetteTheme {
                RetroCassetteApp(viewModel)
            }
        }
    }
}
