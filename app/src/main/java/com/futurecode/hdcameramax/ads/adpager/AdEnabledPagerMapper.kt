package com.futurecode.hdcameramax.ads.adpager


import java.util.Locale

object AdEnabledPagerMapper {

    fun <T> build(
        contentItems: List<T>,
        config: AdPagerPlacementConfig
    ): List<AdPagerItem<T>> {
        if (contentItems.isEmpty()) return emptyList()

        val pagerItems = mutableListOf<AdPagerItem<T>>()
        val lastContentIndex = contentItems.lastIndex

        contentItems.forEachIndexed { contentIndex, item ->
            pagerItems.add(
                AdPagerItem.Content(
                    contentIndex = contentIndex,
                    data = item
                )
            )

            val nextPagerPosition = pagerItems.size
            val adType = config.adTypeFor(
                contentIndex = contentIndex,
                nextPagerPosition = nextPagerPosition,
                lastContentIndex = lastContentIndex
            ) ?: return@forEachIndexed

            pagerItems.add(
                AdPagerItem.NativeAd(
                    pageKey = "ad_${contentIndex}_${nextPagerPosition}_${adType.name.lowercase(Locale.US)}",
                    adType = adType,
                    unlockDurationMs = config.timerUnlockDurationMs
                )
            )
        }

        return pagerItems
    }
}