package com.fws.livebetter

import android.app.Activity
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/** Trạng thái đếm quảng cáo; sống trong ViewModel nên không mất khi xoay màn hình / đổi ngôn ngữ (Activity bị tạo lại). */
class AdCounter {
    val seenItems = HashSet<Int>()   // các mục đã xem trong phiên hiện tại
    var newViews = 0                 // số mục MỚI đã xem kể từ lần hiện quảng cáo xen kẽ gần nhất
    var coldStartHandled = false     // quảng cáo mở ứng dụng lúc khởi động chỉ xét 1 lần mỗi lần mở app
    var lastShownAt = 0L             // elapsedRealtime của lần hiện quảng cáo toàn màn hình gần nhất
}

/**
 * Quảng cáo toàn màn hình của AdMob:
 *  • Mở ứng dụng (App Open): khi mở app (nếu tải kịp) và khi quay lại app từ nền.
 *  • Xen kẽ (Interstitial): hiện khi người dùng xem đủ [INTERSTITIAL_EVERY] mục MỚI.
 * Hai loại cách nhau tối thiểu [MIN_GAP_MS] để không bị hiện chồng nhau.
 * Chỉ gọi [start] sau khi đã có đồng ý (UMP) và MobileAds.initialize.
 */
class FullScreenAds(private var activity: Activity?, private val counter: AdCounter) {
    private var started = false
    private var showing = false
    private var inForeground = false
    private var wasInBackground = false
    private var coldStartPending = false
    private var startedAt = 0L

    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var loadingAppOpen = false

    private var interstitial: InterstitialAd? = null
    private var interstitialLoadedAt = 0L
    private var loadingInterstitial = false

    private fun now() = SystemClock.elapsedRealtime()
    private fun tooSoon() = counter.lastShownAt != 0L && now() - counter.lastShownAt < MIN_GAP_MS
    private fun appOpenFresh() = appOpen != null && now() - appOpenLoadedAt < APP_OPEN_TTL_MS
    private fun interstitialFresh() = interstitial != null && now() - interstitialLoadedAt < INTERSTITIAL_TTL_MS

    fun start() {
        if (started) return
        started = true
        startedAt = now()
        coldStartPending = !counter.coldStartHandled
        counter.coldStartHandled = true
        loadAppOpen()
        loadInterstitial()
    }

    fun onForeground() {
        val returned = wasInBackground
        wasInBackground = false
        inForeground = true
        if (returned && started) {
            if (appOpenFresh()) showAppOpen() else loadAppOpen()
        }
    }

    fun onBackground() {
        inForeground = false
        wasInBackground = true
    }

    /** Gọi mỗi khi một mục được mở; chỉ mục chưa xem trong phiên mới được tính. */
    fun onItemViewed(id: Int) {
        if (!started) return
        if (!counter.seenItems.add(id)) return
        counter.newViews++
        maybeShowInterstitial()
    }

    fun release() {
        activity = null
        appOpen = null
        interstitial = null
    }

    // ───────── App Open ─────────
    private fun loadAppOpen() {
        val a = activity ?: return
        if (loadingAppOpen || appOpenFresh()) return
        loadingAppOpen = true
        AppOpenAd.load(
            a, BuildConfig.ADMOB_APP_OPEN_ID, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadingAppOpen = false
                    appOpen = ad
                    appOpenLoadedAt = now()
                    if (coldStartPending) {
                        coldStartPending = false
                        // Chỉ hiện lúc khởi động nếu tải xong trong vài giây đầu, tránh bật ra giữa lúc đang dùng.
                        if (now() - startedAt <= COLD_START_WINDOW_MS) showAppOpen()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingAppOpen = false
                    coldStartPending = false
                }
            },
        )
    }

    private fun showAppOpen() {
        val a = activity ?: return
        val ad = appOpen ?: return
        if (showing || !inForeground || tooSoon()) return
        appOpen = null
        ad.fullScreenContentCallback = callbacks { loadAppOpen() }
        showing = true
        ad.show(a)
    }

    // ───────── Interstitial ─────────
    private fun loadInterstitial() {
        val a = activity ?: return
        if (loadingInterstitial || interstitialFresh()) return
        loadingInterstitial = true
        InterstitialAd.load(
            a, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    interstitial = ad
                    interstitialLoadedAt = now()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                }
            },
        )
    }

    private fun maybeShowInterstitial() {
        if (counter.newViews < INTERSTITIAL_EVERY || showing || !inForeground) return
        val a = activity ?: return
        if (!interstitialFresh()) { interstitial = null; loadInterstitial(); return }
        if (tooSoon()) return            // giữ nguyên bộ đếm, thử lại ở mục mới kế tiếp
        val ad = interstitial ?: return
        interstitial = null
        counter.newViews = 0
        ad.fullScreenContentCallback = callbacks { loadInterstitial() }
        showing = true
        ad.show(a)
    }

    private fun callbacks(onDone: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() { counter.lastShownAt = now() }
        override fun onAdDismissedFullScreenContent() { showing = false; counter.lastShownAt = now(); onDone() }
        override fun onAdFailedToShowFullScreenContent(error: AdError) { showing = false; onDone() }
    }

    companion object {
        const val INTERSTITIAL_EVERY = 5                       // số mục mới tối thiểu giữa 2 lần hiện quảng cáo xen kẽ
        private const val MIN_GAP_MS = 60_000L                 // khoảng cách tối thiểu giữa 2 quảng cáo toàn màn hình
        private const val COLD_START_WINDOW_MS = 5_000L        // quảng cáo mở ứng dụng lúc khởi động phải tải xong trong 5 giây
        private const val APP_OPEN_TTL_MS = 4 * 3_600_000L     // quảng cáo mở ứng dụng hết hạn sau 4 giờ
        private const val INTERSTITIAL_TTL_MS = 3_600_000L     // quảng cáo xen kẽ hết hạn sau 1 giờ
    }
}
