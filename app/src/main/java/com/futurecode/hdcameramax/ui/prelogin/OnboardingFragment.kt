package com.futurecode.hdcameramax.ui.prelogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.adapter.OnboardingAdapter
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentOnboardingBinding
import com.futurecode.hdcameramax.model.OnboardingSlide // FIXED: Data model ka import add kiya

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {
    private lateinit var onboardingAdapter: OnboardingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Prepare data matching your exact 4 screenshot items
        // FIXED: 'OnboardingAdapter.OnboardingSlide' ki jagah direct standalone 'OnboardingSlide' call kiya

        val slidesData = listOf(
            OnboardingSlide(
                R.drawable.onboarding_one,
                getString(R.string.my_personal_gallery),
                getString(R.string.save_view_and_manage_all_your_photos_and_videos_easily)
            ),
            OnboardingSlide(
                R.drawable.onboarding_two,
                getString(R.string.zoom_with_clarity),
                getString(R.string.zoom_in_or_out_to_capture_sharp_detailed_photos_and_videos)
            ),
            OnboardingSlide(
                R.drawable.onboarding_three,
                getString(R.string.perfect_focus_every_time),
                getString(R.string.lock_focus_instantly_and_capture_sharp_subjects_with_professional_precision)
            ),
            OnboardingSlide(
                R.drawable.onboarding_four,
                getString(R.string.creative_photo_filters),
                getString(R.string.apply_stunning_filters_and_effects_to_give_every_photo_a_unique_and_professional_look)
            )
        )

        // 2. Setup Adapter and attach to ViewPager2
        onboardingAdapter = OnboardingAdapter(slidesData)
        binding.viewPagerOnboarding.adapter = onboardingAdapter

        // 3. Sync sliding dot indicators with ViewPager views
        TabLayoutMediator(binding.onboardingIndicators, binding.viewPagerOnboarding) { _, _ ->
            // Dots are automatically generated and linked with state loops
        }.attach()

        // 4. Handle Page Change Events to switch Button text dynamically
        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == slidesData.size - 1) {
                    binding.btnOnboardingAction.text = getString(R.string.get_started)
                } else {
                    binding.btnOnboardingAction.text = getString(R.string.next)
                }
            }
        })

        // 5. Button Click Control Navigation Routing
        binding.btnOnboardingAction.setOnClickListener {
            val currentPos = binding.viewPagerOnboarding.currentItem
            if (currentPos < slidesData.size - 1) {
                binding.viewPagerOnboarding.currentItem = currentPos + 1
            } else {
                navigateToHomeScreen()
            }
        }
    }

    private fun navigateToHomeScreen() {
        if (!isAdded) return
        findNavController().navigate(R.id.action_onboardingFragment_to_welcomeFragment)
        // Custom Routing logic triggers here cleanly
    }
}