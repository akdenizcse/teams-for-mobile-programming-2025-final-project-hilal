package com.example.recipes

import android.app.Application
import com.google.android.material.color.DynamicColors

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Applies Material You dynamic colors (Android 12+)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}