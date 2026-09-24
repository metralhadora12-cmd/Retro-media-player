package com.retro.cassetteplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.retro.cassetteplayer.ui.RetroCassetteApp
import com.retro.cassetteplayer.ui.theme.RetroCassetteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetroCassetteTheme {
                RetroCassetteApp(viewModel)
            }
        }
    }
}
