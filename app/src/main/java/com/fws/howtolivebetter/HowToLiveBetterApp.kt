package com.fws.howtolivebetter

import android.app.Application
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class HowToLiveBetterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyNightMode(Prefs(this).theme)
        // Lần đầu mở app: tạm dùng ngôn ngữ máy nếu app hỗ trợ (không thì tiếng Anh) làm lựa chọn gợi ý;
        // popup chọn ngôn ngữ (LanguagePickerDialog) sẽ cho người dùng xác nhận hoặc đổi.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            val key = langKey(Resources.getSystem().configuration.locales[0])
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tagForKey(key)))
        }
    }

    companion object {
        /** Khóa ngôn ngữ nội bộ → mã locale áp dụng cho app. */
        fun tagForKey(key: String): String = when (key) {
            "zh" -> "zh-CN"
            "pt" -> "pt-BR"
            else -> key
        }

        fun applyNightMode(theme: String) = AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
