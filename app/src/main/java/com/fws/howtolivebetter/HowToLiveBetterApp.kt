package com.fws.howtolivebetter

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class HowToLiveBetterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyNightMode(Prefs(this).theme)
    }

    companion object {
        fun applyNightMode(theme: String) = AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
