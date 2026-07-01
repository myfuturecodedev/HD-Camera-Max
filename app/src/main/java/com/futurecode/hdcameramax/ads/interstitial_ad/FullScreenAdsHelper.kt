package com.futurecode.hdcameramax.ads.interstitial_ad

import android.app.Activity
import android.util.Log
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.ads.AdInterface
import com.futurecode.hdcameramax.utils.PrefManager
import com.futurecode.hdcameramax.utils.ProgressBarUtils
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback


class FullScreenAdsHelper(private val activity: Activity) {

    private var myPreferenceHelper: PrefManager = MyApplication.app.prefManager
    private lateinit var adInterface: AdInterface

    // FIXED: Changed from lateinit to nullable types to cleanly avoid UninitializedPropertyAccessExceptions
    private var fb_interstitialAd: InterstitialAd? = null
    private var mInterstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd? = null

    fun showInterstitialAds(isShowEveryTime: Boolean, adInterface: AdInterface) {
        this.adInterface = adInterface
        try {
            val limit = myPreferenceHelper.adFrequency
            val count: Int = myPreferenceHelper.adShowCounter

            if (isShowEveryTime) {
                showAds()
            } else {
                if (count % limit == 0) {
                    showAds()
                } else {
                    openActivity()
                }
                myPreferenceHelper.adShowCounter++
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showAdmobAds()
        }
    }

    private fun showAds() {
        if (!myPreferenceHelper.adsOff) {
            when (InterstitialAdsLogic.getCurrentAdNetwork(activity)) {
                "Admob" -> showAdmobAds()
                "Meta" -> showMetaAds()
                "Custom" -> showCustomAd(true)
                else -> openActivity()
            }
        } else {
            openActivity()
        }
    }

    private fun showMetaAds() {
        val interstitialId: String? = myPreferenceHelper.metaInterstitial
        ProgressBarUtils.showProgressDialog(activity)

        val ad = InterstitialAd(activity, interstitialId)
        fb_interstitialAd = ad

        val interstitialAdListener: InterstitialAdListener = object : InterstitialAdListener {
            override fun onInterstitialDisplayed(ad: Ad?) {}

            override fun onInterstitialDismissed(ad: Ad?) {
                ProgressBarUtils.hideProgressDialog()
                openActivity()
            }

            override fun onError(ad: Ad?, adError: AdError?) {
                // FIXED: Dismiss current loader state before attempting fallbacks
                ProgressBarUtils.hideProgressDialog()
                showCustomAd(false)
            }

            override fun onAdLoaded(ad: Ad?) {
                ProgressBarUtils.hideProgressDialog()
                fb_interstitialAd?.show()
            }

            override fun onAdClicked(ad: Ad?) {}
            override fun onLoggingImpression(ad: Ad?) {}
        }

        ad.loadAd(
            ad.buildLoadAdConfig()
                .withAdListener(interstitialAdListener)
                .build()
        )
    }

    private fun showAdmobAds() {
        ProgressBarUtils.showProgressDialog(activity)
        val adRequest = AdRequest.Builder().build()

        com.google.android.gms.ads.interstitial.InterstitialAd.load(
            activity,
            myPreferenceHelper.admobInterstitial,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    // FIXED: Clear processing animation loops to prevent freezing bugs
                    ProgressBarUtils.hideProgressDialog()
                    showCustomAd(false)
                }

                override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                    Log.d("TAG_ADMOB", "Ad was loaded.")
                    mInterstitialAd = interstitialAd

                    mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d("TAG_ADMOB", "Ad was clicked.")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d("TAG_ADMOB", "Ad dismissed fullscreen content.")
                            ProgressBarUtils.hideProgressDialog()
                            openActivity()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                            Log.e("TAG_ADMOB", "Ad failed to show fullscreen content.")
                            ProgressBarUtils.hideProgressDialog()
                            showCustomAd(false)
                        }

                        override fun onAdImpression() {
                            Log.d("TAG_ADMOB", "Ad recorded an impression.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d("TAG_ADMOB", "Ad showed fullscreen content.")
                        }
                    }

                    ProgressBarUtils.hideProgressDialog()
                    mInterstitialAd?.show(activity)
                }
            })
    }

    private fun showCustomAd(showLoader: Boolean) {
        if (showLoader) {
            ProgressBarUtils.showProgressDialog(activity)
        }
        if (!myPreferenceHelper.customOff) {
            CustomFullScreenAdsHelper(activity).show(object : AdInterface {
                override fun finished() {
                    ProgressBarUtils.hideProgressDialog()
                    openActivity()
                }
            })
        } else {
            ProgressBarUtils.hideProgressDialog()
            openActivity()
        }
    }

    private fun openActivity() {
        ProgressBarUtils.hideProgressDialog()
        // FIXED: Safe call operator used to destroy the Meta view if instantiated, completely crash-proof
        fb_interstitialAd?.destroy()
        fb_interstitialAd = null
        adInterface.finished()
    }
}