package com.nurquran.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nurquran.app.ui.NurQuranRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        NurQuranRootContent()
    }

    private fun NurQuranRootContent() {
        val container = (application as NurQuranApplication).container
        setContent { NurQuranRoot(container = container) }
    }
}
