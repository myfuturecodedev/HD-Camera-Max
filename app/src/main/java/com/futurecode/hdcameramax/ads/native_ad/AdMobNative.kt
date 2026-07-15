package com.futurecode.hdcameramax.ads.native_ad

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.utils.PrefManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView


class AdMobNative(
    private val context: Context,
    private val myPreferenceHelper: PrefManager
) {

    fun loadNativeAd(
        container: FrameLayout,
        nativeLayoutRes: Int = R.layout.item_native_ad,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        val adId = myPreferenceHelper.admobNative

        if (adId.isEmpty()) {
            onFailed?.invoke(LoadAdError(0, "Ad unit ID is empty", "AdMobNative", null, null))
            return
        }

        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { nativeAd ->
                val adView = LayoutInflater.from(context)
                    .inflate(nativeLayoutRes, null) as NativeAdView
                val responseInfo = nativeAd.responseInfo
                Log.d("ADS_CHECK", "Adapter: ${responseInfo?.mediationAdapterClassName}")
                Log.d("ADS_CHECK", "Response ID: ${responseInfo?.responseId}")
                populateNativeAdView(nativeAd, adView)

                container.removeAllViews()
                container.addView(adView)
                container.visibility = View.VISIBLE

                onLoaded?.invoke()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    container.visibility = View.GONE
                    onFailed?.invoke(error)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Find views using the standard AdMob IDs
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)

        // Assign views to the AdMob NativeAdView container
        adView.mediaView = mediaView
        adView.headlineView = headlineView
        adView.bodyView = bodyView
        adView.callToActionView = ctaView

        // Set data
        headlineView.text = nativeAd.headline
        bodyView.text = nativeAd.body ?: ""
        ctaView.text = nativeAd.callToAction ?: "Install"

        bodyView.visibility = if (nativeAd.body == null) View.GONE else View.VISIBLE
        ctaView.visibility = if (nativeAd.callToAction == null) View.GONE else View.VISIBLE

        // Link the ad to the view
        adView.setNativeAd(nativeAd)
    }
}
