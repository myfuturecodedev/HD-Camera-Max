package com.futurecode.hdcameramax.ads.adpager

data class AdPagerPlacementConfig(
    val adsEnabled: Boolean = true,
    val normalAdPagerPositions: Set<Int> = emptySet(),
    val timerAdPagerPositions: Set<Int> = emptySet(),
    val adAfterEveryContentItems: Int? = null,
    val intervalAdType: AdPagerAdType = AdPagerAdType.NORMAL,
    val timerUnlockDurationMs: Long = DEFAULT_TIMER_UNLOCK_MS,
    val transitionAdDecider: ((contentIndex: Int, nextPagerPosition: Int) -> AdPagerAdType?)? = null
) {
    fun adTypeFor(
        contentIndex: Int,
        nextPagerPosition: Int,
        lastContentIndex: Int
    ): AdPagerAdType? {
        if (!adsEnabled || contentIndex >= lastContentIndex) return null

        transitionAdDecider?.invoke(contentIndex, nextPagerPosition)?.let { return it }

        if (timerAdPagerPositions.contains(nextPagerPosition)) return AdPagerAdType.TIMER
        if (normalAdPagerPositions.contains(nextPagerPosition)) return AdPagerAdType.NORMAL

        val interval = adAfterEveryContentItems
        if (interval != null && interval > 0 && (contentIndex + 1) % interval == 0) {
            return intervalAdType
        }

        return null
    }

    companion object {
        const val DEFAULT_TIMER_UNLOCK_MS = 3_000L
    }
}