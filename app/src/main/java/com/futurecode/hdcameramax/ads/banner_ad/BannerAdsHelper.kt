package com.futurecode.hdcameramax.ads.banner_ad

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.utils.PrefManager
import com.futurecode.hdcameramax.utils.Utils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError

class BannerAdsHelper(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {

    private var myPreferenceHelper: PrefManager = MyApplication.app.prefManager
    private var admobAdView: com.google.android.gms.ads.AdView? = null
    private var isShowingFallback = false

    init {
        showAds(context)
    }

    fun reload() {
        clearBanner()
        showAds(context)
    }

    private fun clearBanner() {
        isShowingFallback = false
        admobAdView?.destroy()
        admobAdView = null
        removeAllViews()
    }

    fun showAds(context: Context) {
        if (!myPreferenceHelper.adsOff) {
            showAdmobBanner(context)
        } else {
            clearBanner()
        }
    }

    private fun showCustomAd(context: Context) {
        if (isShowingFallback) return

        isShowingFallback = true
        removeAllViews()
        orientation = VERTICAL

        val imageView = ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.banner)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(5, 5, 5, 5)
            adjustViewBounds = true
        }

        addView(imageView)

        imageView.setOnClickListener {
            val activity = context as? Activity
            activity?.let { act ->
                Utils.openCustomChrome(act, Utils.getRandomUrls(act))
            }
        }
    }

    private fun showAdmobBanner(context: Context) {
        clearBanner()

        val localAdView = com.google.android.gms.ads.AdView(context)
        admobAdView = localAdView

        localAdView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        addView(localAdView)

        val displayMetrics = resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()

        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            context,
            adWidth
        )

        localAdView.setAdSize(adSize)
        localAdView.adUnitId = myPreferenceHelper.admobBanner

        localAdView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "onAdFailedToLoad: ADMOB ${adError.message}")

                if (admobAdView !== localAdView) return
                showCustomAd(context)
            }
        }

        localAdView.loadAd(AdRequest.Builder().build())
    }

    override fun onDetachedFromWindow() {
        admobAdView?.destroy()
        admobAdView = null
        super.onDetachedFromWindow()
    }

    companion object {
        private const val TAG = "BannerAdManager"
    }
}