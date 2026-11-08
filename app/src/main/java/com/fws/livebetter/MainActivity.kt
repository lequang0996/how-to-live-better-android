package com.fws.livebetter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fws.livebetter.ui.AppRoot
import com.fws.livebetter.ui.LiveBetterTheme
import com.fws.livebetter.ui.LocalSpeech
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private lateinit var speech: SpeechController
    private lateinit var updateManager: AppUpdateManager
    private lateinit var consent: ConsentInformation
    private lateinit var ads: AppOpenAds

    private val adsReady = mutableStateOf(false)
    private val updateReady = mutableStateOf(false)
    private val privacyRequired = mutableStateOf(false)
    private val adsStarted = AtomicBoolean(false)

    private val installListener = InstallStateUpdatedListener { st ->
        if (st.installStatus() == InstallStatus.DOWNLOADED) updateReady.value = true
    }
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        speech = SpeechController(this).also { it.rate = vm.prefs.ttsRate }
        ads = AppOpenAds(this, vm.adState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { vm.onForeground(); ads.onForeground() }
            override fun onStop(owner: LifecycleOwner) { vm.onBackground(); ads.onBackground() }
        })

        setContent {
            LiveBetterTheme {
                CompositionLocalProvider(LocalSpeech provides speech) {
                    AppRoot(
                        vm, adsReady.value, updateReady.value, privacyRequired.value,
                        onRestartUpdate = { updateManager.completeUpdate() },
                        onPrivacyOptions = { UserMessagingPlatform.showPrivacyOptionsForm(this) { } },
                    )
                }
            }
        }
        setupInAppUpdate()
        setupConsentAndAds()
    }

    // ── In-App Update: FLEXIBLE (tải nền, người dùng bấm "Khởi động lại" để cài) ──
    private fun setupInAppUpdate() {
        updateManager = AppUpdateManagerFactory.create(this)
        updateManager.registerListener(installListener)
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                updateReady.value = true
            } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                updateManager.startUpdateFlowForResult(
                    info, updateLauncher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }

    // ── Đồng ý quảng cáo (UMP) → rồi mới khởi tạo AdMob ──
    private fun setupConsentAndAds() {
        consent = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            this, params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { refreshConsentState() } },
            { refreshConsentState() },
        )
        refreshConsentState() // người dùng cũ đã đồng ý từ lần trước
    }

    private fun refreshConsentState() {
        privacyRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consent.canRequestAds() && adsStarted.compareAndSet(false, true)) {
            Thread {
                MobileAds.initialize(this)
                runOnUiThread { adsReady.value = true; ads.start() }
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        updateManager.appUpdateInfo.addOnSuccessListener {
            if (it.installStatus() == InstallStatus.DOWNLOADED) updateReady.value = true
        }
    }

    override fun onDestroy() {
        updateManager.unregisterListener(installListener)
        ads.release()
        if (isFinishing) speech.shutdown()
        super.onDestroy()
    }
}
