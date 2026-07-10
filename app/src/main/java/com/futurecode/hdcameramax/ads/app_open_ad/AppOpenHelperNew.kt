package com.futurecode.hdcameramax.ads.app_open_ad

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.futurecode.hdcameramax.activity.MainActivity
import com.futurecode.hdcameramax.activity.MyApplication
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenHelperNew(
    private val configs: MyApplication
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAd.AppOpenAdLoadCallback? = null
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0

    init {
        configs.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // CRITICAL FIX: Do NOT show app open ad if the user is on the Splash/Main activity
        // This prevents the ad from blocking the app startup and causing a black screen.
        if (currentActivity !is MainActivity) {
            showAdIfAvailable()
        }
    }

    fun fetchAd() {
        if (isAdAvailable()) return

        loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                loadTime = Date().time
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
        }

        val adId = configs.prefManager.admobAppOpen
        if (adId.isNullOrEmpty()) return

        val callback = loadCallback ?: return
        AppOpenAd.load(configs, adId, AdRequest.Builder().build(), callback)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun isAdAvailable(): Boolean = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityDestroyed(activity: Activity) { if (currentActivity === activity) currentActivity = null }

    fun showAdIfAvailable() {
        val activity = currentActivity
        if (!isShowingAd && isAdAvailable() && activity != null && !activity.isFinishing && !activity.isDestroyed) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    fetchAd()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { isShowingAd = false }
                override fun onAdShowedFullScreenContent() { isShowingAd = true }
            }
            appOpenAd?.show(activity)
        } else {
            fetchAd()
        }
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
    override fun onActivityPaused(p0: Activity) {}
    override fun onActivityStopped(p0: Activity) {}
    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    companion object {
        private var isShowingAd = false
    }
}