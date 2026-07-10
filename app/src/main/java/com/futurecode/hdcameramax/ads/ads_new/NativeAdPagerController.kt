package com.futurecode.hdcameramax.ads.ads_new

import com.futurecode.hdcameramax.databinding.ItemOnboardingNativeAdBinding


class NativeAdPagerController(
    private val nativeAdPageLoader: NativeAdPageLoader
) {

    fun bind(pageKey: String, binding: ItemOnboardingNativeAdBinding) {
        if (binding.root.tag == pageKey) return
        binding.root.tag = pageKey
        nativeAdPageLoader.load(binding)
    }
}