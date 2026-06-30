package com.futurecode.hdcameramax.ui.prelogin

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.adapter.OnboardingAdapter
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentOnboardingBinding
import com.futurecode.hdcameramax.model.OnboardingSlide

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {
    private lateinit var onboardingAdapter: OnboardingAdapter
    private var slidesData = listOf<OnboardingSlide>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slidesData = listOf(
            OnboardingSlide(R.drawable.onboarding_one, "My Personal<br/><font color='#5EBC8F'>Gallery</font>", "Save, view, and manage all your\nphotos and videos easily."),
            OnboardingSlide(R.drawable.onboarding_two, "Zoom with<br/><font color='#5EBC8F'>Clarity</font>", "Zoom in or out to capture sharp,\ndetailed photos and videos."),
            OnboardingSlide(R.drawable.onboarding_three, "Perfect Focus<br/><font color='#5EBC8F'>Every Time</font>", "Lock focus instantly and capture sharp\nsubjects with professional precision."),
            OnboardingSlide(R.drawable.onboarding_four, "Creative Photo<br/><font color='#5EBC8F'>Filters</font>", "Apply stunning filters and effects to\ngive every photo a unique and\nprofessional look.")
        )

        setupViewPager()
        setupClickListeners()
    }

    private fun setupViewPager() {
        onboardingAdapter = OnboardingAdapter(slidesData)
        binding.viewPagerOnboarding.adapter = onboardingAdapter

        // FIXED: Build UI indicators dynamically
        setupIndicatorsContainer(slidesData.size)
        setCurrentIndicatorSelected(0)

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Real-time capsule color and layout scale changes
                setCurrentIndicatorSelected(position)

                binding.btnWelcomeBackArrow.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE

                if (position == slidesData.size - 1) {
                    binding.btnOnboardingAction.text = "Get started →"
                } else {
                    binding.btnOnboardingAction.text = "Next →"
                }
            }
        })
    }

    // Dynamic Dot view allocator logic block
    private fun setupIndicatorsContainer(count: Int) {
        binding.onboardingIndicators.removeAllViews()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 0, 8, 0) // Smooth symmetric margins
        }

        for (i in 0 until count) {
            val indicator = ImageView(requireContext())
            indicator.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive))
            indicator.layoutParams = params
            binding.onboardingIndicators.addView(indicator)
        }
    }

    // Changes scale properties elegantly to match image design reference guidelines
    private fun setCurrentIndicatorSelected(index: Int) {
        val childCount = binding.onboardingIndicators.childCount
        for (i in 0 until childCount) {
            val imageView = binding.onboardingIndicators.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_active))
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive))
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnOnboardingAction.setOnClickListener {
            val currentPos = binding.viewPagerOnboarding.currentItem
            if (currentPos < slidesData.size - 1) {
                binding.viewPagerOnboarding.currentItem = currentPos + 1
            } else {
                navigateToLanguageScreen()
            }
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
        findNavController().navigate(R.id.action_onboardingFragment_to_languageFragment)
    }
}