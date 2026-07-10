package com.futurecode.hdcameramax.ads.reward

import android.app.Activity
import android.util.Log
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.utils.PrefManager
import com.futurecode.hdcameramax.utils.ProgressBarUtils


import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardAdsHelper(private val activity: Activity) {

    private var myPreferenceHelper: PrefManager = MyApplication.app.prefManager

    private var TAG = "TAG_REWARD"
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var adInterface: RewardAdInterface? = null


    // Call this to show Rewarded Ad
    fun showRewardAds(adInterface: RewardAdInterface) {
        this.adInterface = adInterface

        if (rewardedAd == null) {
            loadRewardedAd { success ->
                if (success) showAd()
                else adInterface.onAdFailed("Failed to load rewarded ad")
            }
        } else {
            showAd()
        }
    }
    // Load Rewarded Ad
    private fun loadRewardedAd(callback: (Boolean) -> Unit) {
        if (isLoading) return
        isLoading = true
        ProgressBarUtils.showProgressDialog(activity)

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            myPreferenceHelper.admobRewardAd,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("RewardAdsHelper", "Ad failed to load: ${adError.message}")
                    ProgressBarUtils.hideProgressDialog()
                    rewardedAd = null
                    isLoading = false
                    callback(false)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("RewardAdsHelper", "Ad was loaded.")
                    ProgressBarUtils.hideProgressDialog()
                    rewardedAd = ad
                    isLoading = false
                    callback(true)
                }
            }
        )
    }

    // Show Rewarded Ad
    private fun showAd() {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("RewardAdsHelper", "Ad failed to show: ${adError.message}")
                    adInterface?.onAdFailed(adError.message)
                    rewardedAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("RewardAdsHelper", "Ad dismissed.")
                    adInterface?.onAdClosed()
                    rewardedAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("RewardAdsHelper", "Ad shown.")
                    adInterface?.onAdShown()
                }
            }

            ad.show(activity) { rewardItem: RewardItem ->
                Log.d("RewardAdsHelper", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                adInterface?.onUserEarnedReward(rewardItem)
            }
        } ?: run {
            Log.e("RewardAdsHelper", "Ad not ready yet.")
            adInterface?.onAdFailed("Ad not ready")
        }
    }

    interface RewardAdInterface {
        fun onAdShown()
        fun onAdClosed()
        fun onAdFailed(error: String)
        fun onUserEarnedReward(rewardItem: RewardItem)
    }
}