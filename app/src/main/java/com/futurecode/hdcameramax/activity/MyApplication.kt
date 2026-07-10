package com.futurecode.hdcameramax.activity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.facebook.ads.AudienceNetworkAds
import com.futurecode.hdcameramax.ads.app_open_ad.AppOpenHelperNew
import com.futurecode.hdcameramax.utils.JsonReadUtils
import com.futurecode.hdcameramax.utils.NetworkMonitor
import com.futurecode.hdcameramax.utils.PrefManager
import com.google.android.gms.ads.MobileAds
import java.util.Locale
import kotlin.collections.get

//class MyApplication: Application() {
//    lateinit var prefManager: PrefManager
//
//    private var currentActivity: Activity? = null
//
//
//    override fun attachBaseContext(base: Context) {
//        // Safe locale initialization for Application context
//        val lang = PrefManager.get(base).selectedLanguage
//        val locale = Locale(lang)
//        Locale.setDefault(locale)
//
//        val configuration = Configuration(base.resources.configuration)
//        configuration.setLocale(locale)
//        configuration.setLayoutDirection(locale)
//
//        // Using createConfigurationContext is the safe way for attachBaseContext
//        super.attachBaseContext(base.createConfigurationContext(configuration))
//    }
//
//
//
//    override fun onCreate() {
//        super.onCreate()
//        app = this
//        prefManager = PrefManager.get(this)
//
//       // JsonReadUtils.fetchJsonData(this)
//
//
//    }
//
//    companion object {
//        private const val DEFAULT_LANGUAGE = "en"
//        lateinit var app: com.futurecode.scarymonstercallchat.activity.MyApplication
//
//        fun applyLanguage(languageCode: String, persistSelection: Boolean = true) {
//            val normalizedLanguageCode = languageCode.ifBlank { DEFAULT_LANGUAGE }
//
//            if (persistSelection) {
//                app.prefManager.selectedLanguage = normalizedLanguageCode
//                app.prefManager.isLanguageSelectedFirstTime = true
//            }
//
//            Locale.setDefault(Locale.forLanguageTag(normalizedLanguageCode))
//
//            val appLocales = LocaleListCompat.forLanguageTags(normalizedLanguageCode)
//            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocales.toLanguageTags()) {
//                AppCompatDelegate.setApplicationLocales(appLocales)
//            }
//        }
//    }
//
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        val languageCode = app.prefManager.selectedLanguage ?: "en"
//        val locale = Locale(languageCode)
//        newConfig.setLocale(locale)
//        Locale.setDefault(locale)
//    }
//}



class MyApplication : Application() {

    lateinit var prefManager: PrefManager
    // Use nullable to avoid lateinit exceptions if ads are off
    var appOpenHelper: AppOpenHelperNew? = null
    private lateinit var networkMonitor: NetworkMonitor
    private var currentActivity: Activity? = null
   // private lateinit var analytics: FirebaseAnalytics


    override fun onCreate() {
        super.onCreate()
        app = this
        prefManager = PrefManager.get(this)
       // analytics = Firebase.analytics
        applyLanguage(prefManager.selectedLanguage, persistSelection = false)

        Log.d("TAG", "prefManagerOffffff: ${prefManager.adsOff}")

        // 1. Setup Activity Tracker FIRST
        // This is required because NetworkMonitor needs getCurrentActivity() to show the dialog
        setupActivityTracker()

        // 2. Initialize and Start Network Monitoring
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()

        // 3. General Initializations
        // JsonReadUtils.fetchJsonData(this)
        initializeADS()


    }

    private fun setupActivityTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityStopped(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }

            override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
            override fun onActivityResumed(p0: Activity) {}
            override fun onActivityPaused(p0: Activity) {}
            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
            override fun onActivityDestroyed(p0: Activity) {}
        })
    }

    fun getCurrentActivity(): Activity? = currentActivity

    private fun initializeADS() {
        AudienceNetworkAds.initialize(this)
        MobileAds.initialize(this) {

        }

        if (!prefManager.adsOff) {
            appOpenHelper = AppOpenHelperNew(this)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::networkMonitor.isInitialized) {
            networkMonitor.stopMonitoring()
        }
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "en"
        lateinit var app: MyApplication

        fun applyLanguage(languageCode: String, persistSelection: Boolean = true) {
            val normalizedLanguageCode = languageCode.ifBlank { DEFAULT_LANGUAGE }

            if (persistSelection) {
                app.prefManager.selectedLanguage = normalizedLanguageCode
                app.prefManager.isLanguageSelectedFirstTime = true
            }

            Locale.setDefault(Locale.forLanguageTag(normalizedLanguageCode))

            val appLocales = LocaleListCompat.forLanguageTags(normalizedLanguageCode)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocales.toLanguageTags()) {
                AppCompatDelegate.setApplicationLocales(appLocales)
            }
        }
    }
}