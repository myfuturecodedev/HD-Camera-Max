package com.futurecode.hdcameramax.adapter

import android.app.Activity
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.hdcameramax.databinding.ItemOnboardingNativeAdBinding
import com.futurecode.hdcameramax.databinding.ItemOnboardingSlideBinding
import com.futurecode.hdcameramax.model.OnboardingSlide
import com.futurecode.hdcameramax.ads.adpager.AdPagerItem
import com.futurecode.hdcameramax.ads.adpager.AdPagerAdType
import com.futurecode.hdcameramax.ads.adpager.AdPagerTimerController
import com.futurecode.hdcameramax.ads.ads_new.NativeAdPagerController

class OnboardingAdapter(
    private val activity: Activity,
    val list: List<AdPagerItem<OnboardingSlide>>,
    private val nativeAdPagerController: NativeAdPagerController,
    private val timerController: AdPagerTimerController,
    private val onAdAdvanceRequested: (position: Int) -> Unit,
    private val onContentContinueRequested: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val boundNativeHolders = mutableMapOf<String, NativeAdViewHolder>()
    private var selectedPosition = RecyclerView.NO_POSITION

    class ContentViewHolder(val binding: ItemOnboardingSlideBinding) : RecyclerView.ViewHolder(binding.root)

    class NativeAdViewHolder(val binding: ItemOnboardingNativeAdBinding) : RecyclerView.ViewHolder(binding.root) {
        var boundPageKey: String? = null
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = list[position]) {
            is AdPagerItem.Content -> TYPE_ONBOARDING
            is AdPagerItem.NativeAd -> when (item.adType) {
                AdPagerAdType.NORMAL -> TYPE_NORMAL_AD
                AdPagerAdType.TIMER -> TYPE_TIMER_AD
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_NORMAL_AD,
            TYPE_TIMER_AD -> NativeAdViewHolder(
                ItemOnboardingNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> ContentViewHolder(
                ItemOnboardingSlideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = list[position]) {
            is AdPagerItem.Content -> bindOnboarding(holder as ContentViewHolder, item.data)
            is AdPagerItem.NativeAd -> bindNativeAd(holder as NativeAdViewHolder, item)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is NativeAdViewHolder) {
            holder.boundPageKey?.let(timerController::detach)
            holder.boundPageKey?.let(boundNativeHolders::remove)
            holder.boundPageKey = null
        }
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = list.size

    fun isLastContentPage(position: Int): Boolean {
        return position == list.indexOfLast { it is AdPagerItem.Content }
    }

    fun nextPositionAfter(position: Int): Int? {
        val nextPosition = position + 1
        return nextPosition.takeIf { it in list.indices }
    }

    fun release() {
        timerController.clear()
        boundNativeHolders.clear()
    }

    fun onPageSelected(position: Int) {
        selectedPosition = position
        timerController.clear()
        resetBoundTimerPages()
        startTimerIfNeeded(position)
    }

    private fun bindOnboarding(holder: ContentViewHolder, item: OnboardingSlide) {
        holder.binding.apply {
            ivOnboardingArt.setImageResource(item.imageRes)
            tvOnboardingTitle.text = Html.fromHtml(item.title, Html.FROM_HTML_MODE_COMPACT)
            tvOnboardingDesc.text = item.description
        }
    }

    private fun bindNativeAd(holder: NativeAdViewHolder, item: AdPagerItem.NativeAd) {
        holder.boundPageKey = item.pageKey
        boundNativeHolders[item.pageKey] = holder
        nativeAdPagerController.bind(item.pageKey, holder.binding)

        when (item.adType) {
            AdPagerAdType.NORMAL -> bindNormalAdControls(holder)
            AdPagerAdType.TIMER -> bindTimerAdControls(holder, item)
        }
    }

    private fun bindNormalAdControls(holder: NativeAdViewHolder) {
        holder.binding.tvAdStatus.isVisible = true
        holder.binding.tvAdStatus.text = "ADVERTISEMENT"
        holder.binding.btnAdNext.isVisible = true
        holder.binding.btnAdNext.isEnabled = true
        holder.binding.btnAdNext.alpha = ENABLED_ALPHA
        holder.binding.btnAdNext.text = "X"
        holder.binding.btnAdNext.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onAdAdvanceRequested(position)
        }
    }

    private fun bindTimerAdControls(holder: NativeAdViewHolder, item: AdPagerItem.NativeAd) {
        holder.binding.tvAdStatus.isVisible = true
        holder.binding.tvAdStatus.text = "ADVERTISEMENT"
        setTimerLockedState(holder, item.unlockDurationMs)
        holder.binding.btnAdNext.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onAdAdvanceRequested(position)
        }

        if (holder.bindingAdapterPosition == selectedPosition) {
            startTimer(holder, item)
        }
    }

    private fun startTimerIfNeeded(position: Int) {
        val item = list.getOrNull(position) as? AdPagerItem.NativeAd ?: return
        if (item.adType != AdPagerAdType.TIMER) return

        val holder = boundNativeHolders[item.pageKey]
        if (holder == null) {
            notifyItemChanged(position)
            return
        }
        startTimer(holder, item)
    }

    private fun startTimer(holder: NativeAdViewHolder, item: AdPagerItem.NativeAd) {
        setTimerLockedState(holder, item.unlockDurationMs)
        timerController.bind(
            pageKey = item.pageKey,
            durationMs = item.unlockDurationMs,
            onTick = { seconds ->
                holder.binding.tvAdStatus.text = "ADVERTISEMENT"
                holder.binding.btnAdNext.isVisible = true
                holder.binding.btnAdNext.isEnabled = false
                holder.binding.btnAdNext.alpha = ENABLED_ALPHA
                holder.binding.btnAdNext.text = seconds.toString()
            },
            onUnlocked = {
                holder.binding.tvAdStatus.text = "ADVERTISEMENT"
                holder.binding.btnAdNext.isVisible = true
                holder.binding.btnAdNext.text = "X"
                holder.binding.btnAdNext.isEnabled = true
                holder.binding.btnAdNext.alpha = ENABLED_ALPHA
            }
        )
    }

    private fun resetBoundTimerPages() {
        boundNativeHolders.values.forEach { holder ->
            val position = holder.bindingAdapterPosition
            val item = list.getOrNull(position) as? AdPagerItem.NativeAd ?: return@forEach
            if (item.adType == AdPagerAdType.TIMER) {
                setTimerLockedState(holder, item.unlockDurationMs)
            }
        }
    }

    private fun setTimerLockedState(holder: NativeAdViewHolder, durationMs: Long) {
        holder.binding.tvAdStatus.text = "ADVERTISEMENT"
        holder.binding.btnAdNext.isVisible = true
        holder.binding.btnAdNext.isEnabled = false
        holder.binding.btnAdNext.alpha = DISABLED_ALPHA
        holder.binding.btnAdNext.text = ((durationMs + 999L) / 1_000L).toString()
    }

    companion object {
        private const val TYPE_ONBOARDING = 0
        private const val TYPE_NORMAL_AD = 1
        private const val TYPE_TIMER_AD = 2
        private const val DISABLED_ALPHA = 0.55f
        private const val ENABLED_ALPHA = 1f
    }
}
