package com.retro.cassetteplayer

import android.app.Application
import com.retro.cassetteplayer.data.AppSettings
import com.retro.cassetteplayer.data.PlayStats

/** Loads the app-wide settings and listening stats once per process (UI and service share them). */
class RetroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        PlayStats.init(this)
    }
}
