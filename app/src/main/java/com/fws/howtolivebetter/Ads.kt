package com.fws.howtolivebetter

import android.app.Activity
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/** Trạng thái quảng cáo mở ứng dụng; sống trong ViewModel nên không mất khi xoay màn hình / đổi ngôn ngữ (Activity bị tạo lại). */
class AdState {
    var coldStartHandled = false     // quảng cáo lúc khởi động chỉ xét 1 lần mỗi lần mở app
    var lastShownAt = 0L             // elapsedRealtime của lần hiện quảng cáo toàn màn hình gần nhất
}

/**
 * Quảng cáo Mở ứng dụng (App Open) của AdMob: hiện khi mở app (nếu tải kịp) và khi quay lại app từ nền.
 * Chỉ gọi [start] sau khi đã có đồng ý (UMP) và MobileAds.initialize.
 */
class AppOpenAds(private var activity: Activity?, private val state: AdState) {
    private var started = false
    private var showing = false
    private var inForeground = false
    private var wasInBackground = false
    private var coldStartPending = false
    private var startedAt = 0L

    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var loading = false

    private fun now() = SystemClock.elapsedRealtime()
    private fun tooSoon() = state.lastShownAt != 0L && now() - state.lastShownAt < MIN_GAP_MS
    private fun fresh() = appOpen != null && now() - appOpenLoadedAt < TTL_MS

    fun start() {
        if (started) return
        started = true
        startedAt = now()
        coldStartPending = !state.coldStartHandled
        state.coldStartHandled = true
        load()
    }

    fun onForeground() {
        val returned = wasInBackground
        wasInBackground = false
        inForeground = true
        if (returned && started) {
            if (fresh()) show() else load()
        }
    }

    fun onBackground() {
        inForeground = false
        wasInBackground = true
    }

    fun release() {
        activity = null
        appOpen = null
    }

    private fun load() {
        val a = activity ?: return
        if (loading || fresh()) return
        loading = true
        AppOpenAd.load(
            a, BuildConfig.ADMOB_APP_OPEN_ID, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loading = false
                    appOpen = ad
                    appOpenLoadedAt = now()
                    if (coldStartPending) {
                        coldStartPending = false
                        // Chỉ hiện lúc khởi động nếu tải xong trong vài giây đầu, tránh bật ra giữa lúc đang dùng.
                        if (now() - startedAt <= COLD_START_WINDOW_MS) show()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    coldStartPending = false
                }
            },
        )
    }

    private fun show() {
        val a = activity ?: return
        val ad = appOpen ?: return
        if (showing || !inForeground || tooSoon()) return
        appOpen = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { state.lastShownAt = now() }
            override fun onAdDismissedFullScreenContent() { showing = false; state.lastShownAt = now(); load() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { showing = false; load() }
        }
        showing = true
        ad.show(a)
    }

    companion object {
        private const val MIN_GAP_MS = 60_000L                 // chống hiện lại ngay khi vừa đóng quảng cáo (app quay về từ màn quảng cáo)
        private const val COLD_START_WINDOW_MS = 5_000L        // quảng cáo lúc khởi động phải tải xong trong 5 giây
        private const val TTL_MS = 4 * 3_600_000L              // quảng cáo hết hạn sau 4 giờ
    }
}
