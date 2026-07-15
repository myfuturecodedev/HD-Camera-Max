package com.futurecode.hdcameramax.ads.native_ad

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdListener
import com.facebook.ads.NativeAdView
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.utils.PrefManager
import com.futurecode.hdcameramax.utils.Utils
import com.google.android.material.imageview.ShapeableImageView
import kotlin.code


class NativeAdsHelper(private val activity: Activity) {

    private var myPreferenceHelper: PrefManager = MyApplication.app.prefManager
    private lateinit var fbNativeAd: NativeAd

    fun showNativeAd(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ShapeableImageView,
        nativeLayoutRes: Int = R.layout.item_native_ad
    ) {
        if (!myPreferenceHelper.adsOff) {
            nativeBannerAdView.removeAllViews()
            nativeBannerAdView.background = null
            placeholder.visibility = View.VISIBLE

            val network = NativeAdsLogic.getCurrentAdNetwork(activity)
            Log.d("TAG_NATIVE", "network: $network")

            when (network) {
                "Admob" -> showAdmobNative(nativeBannerAdView, mainLayout, placeholder, nativeLayoutRes)
                "Meta" -> showMetaNative(nativeBannerAdView, mainLayout, placeholder)
                "Custom" -> showCustomNative(nativeBannerAdView, mainLayout, placeholder)
                else -> hideAdmobNative(nativeBannerAdView, mainLayout, placeholder)
            }
        } else {
            hideAdmobNative(nativeBannerAdView, mainLayout, placeholder)
        }
    }

    private fun showMetaNative(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ShapeableImageView
    ) {
        val nativeId = myPreferenceHelper.metaNative

        if (nativeId.isNullOrEmpty()) {
            showCustomNative(nativeBannerAdView, mainLayout, placeholder)
            return
        }

        fbNativeAd = NativeAd(activity, nativeId)

        val nativeAdListener = object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad?) {}

            override fun onError(ad: Ad?, adError: AdError?) {
                nativeBannerAdView.removeAllViews()
                showCustomNative(nativeBannerAdView, mainLayout, placeholder)
            }

            override fun onAdLoaded(ad: Ad?) {
                val adView = NativeAdView.render(activity, fbNativeAd)
                nativeBannerAdView.visibility = View.VISIBLE
                mainLayout.visibility = View.GONE
                placeholder.visibility = View.GONE

                nativeBannerAdView.removeAllViews()
                nativeBannerAdView.addView(
                    adView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            override fun onAdClicked(ad: Ad?) {}

            override fun onLoggingImpression(ad: Ad?) {}
        }

        fbNativeAd.loadAd(
            fbNativeAd.buildLoadAdConfig()
                .withAdListener(nativeAdListener)
                .build()
        )
    }

    private fun showCustomNative(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ShapeableImageView
    ) {
        if (myPreferenceHelper.customOff) {
            hideAdmobNative(nativeBannerAdView, mainLayout, placeholder)
            return
        }

        nativeBannerAdView.removeAllViews()
        nativeBannerAdView.visibility = View.GONE
        mainLayout.visibility = View.VISIBLE
        placeholder.visibility = View.VISIBLE
        placeholder.adjustViewBounds = true
        placeholder.scaleType = ImageView.ScaleType.FIT_CENTER

        Glide.with(activity)
            .load(R.drawable.native_thumb)
            .into(placeholder)

        placeholder.setOnClickListener {
            Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
        }
    }

    private fun hideAdmobNative(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ShapeableImageView
    ) {
        nativeBannerAdView.removeAllViews()
        nativeBannerAdView.visibility = View.GONE
        mainLayout.visibility = View.GONE
        placeholder.visibility = View.GONE
    }

    private fun showAdmobNative(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ShapeableImageView,
        nativeLayoutRes: Int
    ) {
        Log.d("TAG_NATIVE", "showAdmobNative")

        if (myPreferenceHelper.admobNative.isNullOrEmpty()) {
            showCustomNative(nativeBannerAdView, mainLayout, placeholder)
            return
        }

        AdMobNative(activity, myPreferenceHelper).loadNativeAd(
            container = nativeBannerAdView,
            nativeLayoutRes = nativeLayoutRes,
            onLoaded = {
                nativeBannerAdView.visibility = View.VISIBLE
                mainLayout.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
            },
            onFailed = { error ->
                nativeBannerAdView.removeAllViews()
                nativeBannerAdView.visibility = View.GONE
                Log.d(
                    "ADMOB_TES",
                    "domain: ${error.domain}, code: ${error.code}, message: ${error.message}"
                )
                showCustomNative(nativeBannerAdView, mainLayout, placeholder)
            }
        )


    }
}
