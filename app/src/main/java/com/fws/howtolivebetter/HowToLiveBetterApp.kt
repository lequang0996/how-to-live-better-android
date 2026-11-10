package com.fws.howtolivebetter

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class HowToLiveBetterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyNightMode(Prefs(this).theme)
        // Không còn tùy chọn "Theo hệ thống": lần đầu mở app (chưa chọn ngôn ngữ) → mặc định tiếng Anh.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
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
