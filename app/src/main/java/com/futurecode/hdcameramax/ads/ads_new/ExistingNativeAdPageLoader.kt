package com.futurecode.hdcameramax.ads.ads_new

import android.app.Activity
import android.view.View
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.databinding.ItemOnboardingNativeAdBinding
import com.futurecode.hdcameramax.utils.Utils


import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

class ExistingNativeAdPageLoader(
    private val activity: Activity
) : NativeAdPageLoader {

    private val prefManager = MyApplication.app.prefManager

    override fun load(binding: ItemOnboardingNativeAdBinding) {
        if (prefManager.adsOff ) {
            showFallback(binding)
            return
        }

        if (prefManager.admobNative.isBlank()) {
            showFallback(binding)
            return
        }

        val adLoader = AdLoader.Builder(activity, prefManager.admobNative)
            .forNativeAd { nativeAd ->
                bindNativeAd(nativeAd, binding)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    showFallback(binding)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun bindNativeAd(
        nativeAd: NativeAd,
        binding: ItemOnboardingNativeAdBinding
    ) {
        binding.adHeadline.visibility = View.VISIBLE
        binding.adBody.visibility = View.VISIBLE
        binding.adIcon.visibility = View.VISIBLE
        binding.adMedia.visibility = View.VISIBLE
        binding.ivFallbackMedia.visibility = View.GONE
        binding.adCallToAction.visibility = View.VISIBLE

        binding.root.mediaView = binding.adMedia
        binding.root.iconView = binding.adIcon
        binding.root.headlineView = binding.adHeadline
        binding.root.bodyView = binding.adBody
        binding.root.callToActionView = binding.adCallToAction

        binding.adHeadline.text = nativeAd.headline.orEmpty()
        binding.adBody.text = nativeAd.body.orEmpty()
        binding.adCallToAction.text = nativeAd.callToAction ?: "LEARN MORE"

        val icon = nativeAd.icon?.drawable
        if (icon != null) {
            binding.adIcon.setImageDrawable(icon)
        } else {
            binding.adIcon.setImageResource(R.drawable.app_logo)
        }

        binding.root.setNativeAd(nativeAd)
    }

    private fun showFallback(binding: ItemOnboardingNativeAdBinding) {
        if (prefManager.customOff) {
            binding.adHeadline.visibility = View.GONE
            binding.adBody.visibility = View.GONE
            binding.adIcon.visibility = View.GONE
            binding.adMedia.visibility = View.GONE
            binding.ivFallbackMedia.visibility = View.GONE
            binding.adCallToAction.visibility = View.GONE
            return
        }

        binding.adHeadline.visibility = View.VISIBLE
        binding.adBody.visibility = View.VISIBLE
        binding.adIcon.visibility = View.VISIBLE
        binding.adMedia.visibility = View.GONE
        binding.ivFallbackMedia.visibility = View.VISIBLE
        binding.adCallToAction.visibility = View.VISIBLE

        binding.adHeadline.text = "Quiz Trivia"
        binding.adBody.text = "Answer Questions & earn rbx coins"
        binding.adCallToAction.text = "LEARN MORE"
        binding.adIcon.setImageResource(R.drawable.app_logo)

        Glide.with(activity)
            .load(R.drawable.native_thumb)
            .into(binding.ivFallbackMedia)

        binding.adIcon.setOnClickListener {
            Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
        }
        binding.ivFallbackMedia.setOnClickListener {
            Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
        }
        binding.adCallToAction.setOnClickListener {
            Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
        }
    }
}
