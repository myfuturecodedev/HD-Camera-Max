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
import kotlin.inc


class FullScreenAdsHelper(private val activity: Activity) {

    private var myPreferenceHelper: PrefManager = MyApplication.app.prefManager
    private lateinit var adInterface: AdInterface
    private lateinit var fb_interstitialAd: InterstitialAd
    private lateinit var mInterstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd

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
        if (interstitialId.isNullOrBlank()) {
            showCustomAd(false)
            return
        }
        ProgressBarUtils.showProgressDialog(activity)
        fb_interstitialAd = InterstitialAd(activity, interstitialId)
        val interstitialAdListener: InterstitialAdListener = object : InterstitialAdListener {
            override fun onInterstitialDisplayed(ad: Ad?) {
                // Interstitial ad displayed callback
            }

            override fun onInterstitialDismissed(ad: Ad?) {
                // Interstitial dismissed callback
                ProgressBarUtils.hideProgressDialog()
                openActivity()
            }

            override fun onError(ad: Ad?, adError: AdError?) {
                // Ad error callback
                showCustomAd(false)
            }

            override fun onAdLoaded(ad: Ad?) {
                // Interstitial ad is loaded and ready to be displayed
                // Show the ad
                ProgressBarUtils.hideProgressDialog()
                fb_interstitialAd.show()
            }

            override fun onAdClicked(ad: Ad?) {
                // Ad clicked callback
            }

            override fun onLoggingImpression(ad: Ad?) {
                // Ad impression logged callback
            }
        }


        // For auto play video ads, it's recommended to load the ad
        // at least 30 seconds before it is shown
        fb_interstitialAd.loadAd(
            fb_interstitialAd.buildLoadAdConfig()
                .withAdListener(interstitialAdListener)
                .build()
        )

    }

    private fun showAdmobAds() {
        val interstitialId = myPreferenceHelper.admobInterstitial
        if (interstitialId.isNullOrBlank()) {
            showCustomAd(false)
            return
        }
        ProgressBarUtils.showProgressDialog(activity)
        val adRequest = AdRequest.Builder().build()
        com.google.android.gms.ads.interstitial.InterstitialAd.load(
            activity,
            interstitialId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    showCustomAd(false)


                }

                override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                    Log.d("TAG_ADMOB", "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    val responseInfo = mInterstitialAd.responseInfo
                    Log.d("ADS_CHECK", "Adapter: ${responseInfo?.mediationAdapterClassName}")
                    Log.d("ADS_CHECK", "Response ID: ${responseInfo?.responseId}")
                    mInterstitialAd.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdClicked() {
                                // Called when a click is recorded for an ad.
                                Log.d("TAG_ADMOB", "Ad was clicked.")
                            }

                            override fun onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                Log.d("TAG_ADMOB", "Ad dismissed fullscreen content.")
                                ProgressBarUtils.hideProgressDialog()
                                openActivity()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                                // Called when ad fails to show.
                                Log.e("TAG_ADMOB", "Ad failed to show fullscreen content.")
                                showCustomAd(false)
                            }

                            override fun onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d("TAG_ADMOB", "Ad recorded an impression.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                Log.d("TAG_ADMOB", "Ad showed fullscreen content.")
                            }
                        }
                    if (this@FullScreenAdsHelper::mInterstitialAd.isInitialized) {
                        ProgressBarUtils.hideProgressDialog()
                        mInterstitialAd.show(activity)
                    } else {
                        showCustomAd(false)
                    }
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
        if (this::fb_interstitialAd.isInitialized && fb_interstitialAd != null) {
            fb_interstitialAd.destroy()
        }

        adInterface.finished()
    }


}