package com.futurecode.hdcameramax.ui.prelogin

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.adapter.OnboardingAdapter
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentOnboardingBinding
import com.futurecode.hdcameramax.model.OnboardingSlide

import com.futurecode.hdcameramax.ads.adpager.AdEnabledPagerMapper
import com.futurecode.hdcameramax.ads.adpager.AdPagerPlacementConfig
import com.futurecode.hdcameramax.ads.adpager.AdPagerItem
import com.futurecode.hdcameramax.ads.adpager.AdPagerTimerController
import com.futurecode.hdcameramax.ads.ads_new.NativeAdPagerController
import com.futurecode.hdcameramax.ads.ads_new.ExistingNativeAdPageLoader

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {

    private lateinit var onboardingAdapter: OnboardingAdapter
    private var mixedPagerItems = listOf<AdPagerItem<OnboardingSlide>>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (::onboardingAdapter.isInitialized) {
                onboardingAdapter.onPageSelected(position)
                updateUIForPosition(position)
            }
        }
    }

    private val nativeAdPagerController by lazy {
        NativeAdPagerController(ExistingNativeAdPageLoader(requireActivity()))
    }

    private val timerController by lazy {
        AdPagerTimerController()
    }

    private val initialPagerSyncRunnable = Runnable {
        val currentBinding = bindingOrNull ?: return@Runnable
        if (::onboardingAdapter.isInitialized && view != null) {
            val currentPos = currentBinding.viewPagerOnboarding.currentItem
            onboardingAdapter.onPageSelected(currentPos)
            updateUIForPosition(currentPos)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val slidesData = listOf(
            OnboardingSlide(R.drawable.onboarding_one, "My Personal<br/><font color='#5EBC8F'>Gallery</font>", "Save, view, and manage all your\nphotos and videos easily."),
            OnboardingSlide(R.drawable.onboarding_two, "Zoom with<br/><font color='#5EBC8F'>Clarity</font>", "Zoom in or out to capture sharp,\ndetailed photos and videos."),
            OnboardingSlide(R.drawable.onboarding_three, "Perfect Focus<br/><font color='#5EBC8F'>Every Time</font>", "Lock focus instantly and capture sharp\nsubjects with professional precision."),
            OnboardingSlide(R.drawable.onboarding_four, "Creative Photo<br/><font color='#5EBC8F'>Filters</font>", "Apply stunning filters and effects to\ngive every photo a unique and\nprofessional look.")
        )

        val isAdsEnabled = !MyApplication.app.prefManager.adsOff

        val adConfig = AdPagerPlacementConfig(
            adsEnabled = isAdsEnabled,
            timerAdPagerPositions = setOf(1, 3,5),
            timerUnlockDurationMs = 3_000L
        )

        mixedPagerItems = AdEnabledPagerMapper.build(slidesData, adConfig)
        setupViewPager()
        setupClickListeners()
    }

    private fun setupViewPager() {
        onboardingAdapter = OnboardingAdapter(
            activity = requireActivity(),
            list = mixedPagerItems,
            nativeAdPagerController = nativeAdPagerController,
            timerController = timerController,
            onAdAdvanceRequested = { position ->
                onboardingAdapter.nextPositionAfter(position)?.let { nextPosition ->
                    binding.viewPagerOnboarding.setCurrentItem(nextPosition, true)
                }
            },
            onContentContinueRequested = { position ->
                handleContinue(position)
            }
        )

        binding.viewPagerOnboarding.adapter = onboardingAdapter
        binding.viewPagerOnboarding.offscreenPageLimit = 1
        binding.viewPagerOnboarding.registerOnPageChangeCallback(pageChangeCallback)

        val cleanContentCount = mixedPagerItems.count { it is AdPagerItem.Content }
        setupIndicatorsContainer(cleanContentCount)

        binding.viewPagerOnboarding.post(initialPagerSyncRunnable)
    }

    private fun updateUIForPosition(position: Int) {
        val currentBinding = bindingOrNull ?: return
        if (!isAdded) return

        val item = mixedPagerItems.getOrNull(position)
        if (item is AdPagerItem.Content) {
            currentBinding.btnOnboardingAction.visibility = View.VISIBLE
            currentBinding.onboardingIndicators.visibility = View.VISIBLE

            if (position == mixedPagerItems.size - 1) {
                currentBinding.btnOnboardingAction.text = "Get started →"
            } else {
                currentBinding.btnOnboardingAction.text = "Next →"
            }

            updateSymmetricSelectedCapsuleDot(position)
        } else {
            currentBinding.btnOnboardingAction.visibility = View.GONE
            currentBinding.onboardingIndicators.visibility = View.GONE
        }

        currentBinding.btnWelcomeBackArrow.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
    }

    private fun handleContinue(position: Int) {
        if (onboardingAdapter.isLastContentPage(position)) {
            navigateToLanguageScreen()
        } else {
            onboardingAdapter.nextPositionAfter(position)?.let { nextPosition ->
                binding.viewPagerOnboarding.setCurrentItem(nextPosition, true)
            }
        }
    }

    private fun setupIndicatorsContainer(count: Int) {
        binding.onboardingIndicators.removeAllViews()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 0, 8, 0)
        }

        for (i in 0 until count) {
            val indicator = ImageView(requireContext())
            indicator.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive))
            indicator.layoutParams = params
            binding.onboardingIndicators.addView(indicator)
        }
    }

    private fun updateSymmetricSelectedCapsuleDot(currentGlobalPosition: Int) {
        val childCount = binding.onboardingIndicators.childCount
        if (childCount == 0) return

        val contentIndices = mixedPagerItems.indices.filter { mixedPagerItems[it] is AdPagerItem.Content }
        val visualActiveIndex = contentIndices.indexOf(currentGlobalPosition)

        for (i in 0 until childCount) {
            val imageView = binding.onboardingIndicators.getChildAt(i) as? ImageView ?: continue
            if (i == visualActiveIndex) {
                imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_active))
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive))
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnOnboardingAction.setOnClickListener {
            handleContinue(binding.viewPagerOnboarding.currentItem)
        }

        binding.btnWelcomeBackArrow.setOnClickListener {
            val currentPos = binding.viewPagerOnboarding.currentItem
            if (currentPos > 0) {
                binding.viewPagerOnboarding.currentItem = currentPos - 1
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun navigateToLanguageScreen() {
        if (!isAdded) return
        findNavController().navigate(R.id.action_onboardingFragment_to_permissionFragment)
    }

    override fun onDestroyView() {
        bindingOrNull?.viewPagerOnboarding?.removeCallbacks(initialPagerSyncRunnable)
        bindingOrNull?.viewPagerOnboarding?.unregisterOnPageChangeCallback(pageChangeCallback)
        if (::onboardingAdapter.isInitialized) {
            onboardingAdapter.release()
        }
        super.onDestroyView()
    }
}
