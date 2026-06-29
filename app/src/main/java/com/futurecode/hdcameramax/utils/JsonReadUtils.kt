package com.futurecode.hdcameramax.utils

import android.content.Context
import android.util.Log
import com.futurecode.hdcameramax.BuildConfig
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.api.NetworkModule.apiService
import com.futurecode.hdcameramax.model.CommonAdsModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object JsonReadUtils {
    fun fetchJsonData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val FOLDER_NAME = BuildConfig.APPLICATION_ID
                val APP_KEY ="MpAEWpiXPcJYuZNcEZXqlXdQWJuEEd"
                val response = apiService.getJson(FOLDER_NAME, APP_KEY)
                if (response.isSuccessful){
                    val rawJson = response.body()?.string() ?: "{}"
                    Log.d("TAG_JSON", "Raw JSON: $rawJson")
                    // Parse manually
                    val gson = GsonBuilder().disableHtmlEscaping().create()

                    val apiResponse = gson.fromJson(rawJson, CommonAdsModel::class.java)
                    if (apiResponse.meta?.code == 200) {
                        val model = apiResponse.data
                        val prefManager = MyApplication.app.prefManager
                        model?.let {
                            // Do something with the upiValue here
                            model?.let { it ->
                                prefManager.adFrequency = it.ads?.adsFrequency ?: 0
                                prefManager.clickLimit = it.appSettings?.inappbannerClickLimit ?: 0

                                prefManager.clickUrl = it.appSettings?.notificationUrl ?: ""
                                prefManager.admobBanner = it.ads?.admobBanner ?: ""
                                prefManager.admobInterstitial = it.ads?.admobInterstitial ?: ""
                                prefManager.admobNative = it.ads?.admobNative ?: ""
                                prefManager.admobRewardAd = it.ads?.admobReward ?: ""
                                prefManager.admobAppOpen = it.ads?.admobAppOpen ?: ""

                                prefManager.metaInterstitial = it.ads?.metaInterstitial ?: ""
                                prefManager.metaNative = it.ads?.metaNative ?: ""

                                prefManager.contactUs = it.appSettings?.contactUs ?: ""
                                prefManager.privacyPolicy = it.appSettings?.privacyPolicy ?: ""
                                prefManager.notificationUrl = it.appSettings?.notificationUrl ?: ""

                                prefManager.customUrls = Gson().toJson(
                                    it.ads?.customUrl?.filterNotNull() ?: emptyList<String>()
                                )

                                prefManager.adFlow = Gson().toJson(
                                    it.ads?.interAdsFlow?.filterNotNull() ?: emptyList<String>()
                                )

                                prefManager.nativeAdFlow = Gson().toJson(
                                    it.ads?.nativeAdsFlow?.filterNotNull() ?: emptyList<String>()
                                )

                                prefManager.nativeBannerAdFlow = Gson().toJson(
                                    it.ads?.nativeBannerAdsFlow?.filterNotNull() ?: emptyList<String>()
                                )
                                prefManager.notificationList = Gson().toJson(
                                    it.appSettings?.notifications?.filterNotNull() ?: emptyList<String>()
                                )

                                prefManager.adsOff = !it.ads?.adsStatus.equals("on", true)
                                prefManager.customOff = !it.ads?.customAdsStatus.equals("on", true)

//                                prefManager.adsOff = it.ads?.adsStatus.equals("off", true)
//                                prefManager.customOff = it.ads?.customAdsStatus.equals("off", true)

                                prefManager.notificationList = Gson().toJson(
                                    it.appSettings?.notifications?.filterNotNull() ?: emptyList<String>()
                                )

                                prefManager.adNetworkIndex = 0
                                prefManager.nativeAdNetworkIndex = 0
                                prefManager.nativeBannerAdNetworkIndex = 0
                                prefManager.adShowCounter = 0
                                prefManager.clickCount = 0

                                prefManager.weekly=it.appSettings?.plan1Text.toString()
                                prefManager.monthly=it.appSettings?.plan2Text.toString()
                                prefManager.quartly=it.appSettings?.plan3Text.toString()

                                Log.d("TAG_FIREBASE", "upiValue: ${Gson().toJson(model)}")
                            }


                            Log.d("TAG_FIREBASE", "upiValue: ${Gson().toJson(model)}")
                        }
                    } else {
                        Log.e("TAG_JSON", "Error: ${apiResponse.meta?.status}")
                    }
                }else{
                    Log.e("TAG_JSON", "Error: ${response.message()}")

                }

            } catch (e: Exception) {
                Log.e("TAG_JSON", "Exception: ${e.localizedMessage}")
            }
        }
    }


}