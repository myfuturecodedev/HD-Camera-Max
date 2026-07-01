package com.futurecode.hdcameramax.ads.interstitial_ad

import android.app.Activity
import android.util.Log
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.utils.PrefManager
import com.futurecode.hdcameramax.utils.Utils
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdsLogic {
    private var adNetworkIndex = 0
    private var myPreferenceHelper: PrefManager = MyApplication.app.prefManager


    fun getCurrentAdNetwork(activity: Activity): String {
        try {
//            return "Custom"
            val adNetworks: List<String> = Utils.jsonToStringList(myPreferenceHelper.adFlow)


            adNetworkIndex = myPreferenceHelper.adNetworkIndex
            val currentAdNetwork = adNetworks[adNetworkIndex]

            // Increment the index for the next ad network
            adNetworkIndex = (adNetworkIndex + 1) % adNetworks.size
            myPreferenceHelper.adNetworkIndex = adNetworkIndex

            return currentAdNetwork
        } catch (e: Exception) {
            e.printStackTrace()
            return "Admob"
        }

    }

}