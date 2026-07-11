package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.ui.BrowserScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel

import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable 120Hz high refresh rate / smooth display mode if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val window = window
                val layoutParams = window.attributes
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                if (display != null) {
                    val supportedModes = display.supportedModes
                    var bestMode = display.mode
                    var maxRefreshRate = 60f
                    for (mode in supportedModes) {
                        if (mode.refreshRate > maxRefreshRate) {
                            maxRefreshRate = mode.refreshRate
                            bestMode = mode
                        }
                    }
                    if (maxRefreshRate > 60f) {
                        layoutParams.preferredDisplayModeId = bestMode.modeId
                        window.attributes = layoutParams
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        viewModel = ViewModelProvider(
            this, 
            BrowserViewModel.Factory(application)
        )[BrowserViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (::viewModel.isInitialized) {
            viewModel.setIsInPictureInPictureMode(isInPictureInPictureMode)
        }
    }
}

