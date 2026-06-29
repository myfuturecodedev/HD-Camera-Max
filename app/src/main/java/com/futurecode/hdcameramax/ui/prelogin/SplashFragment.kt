package com.futurecode.hdcameramax.ui.prelogin

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MainActivity
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentSplashBinding
import com.futurecode.hdcameramax.utils.PrefManager

class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Progress bar aur dynamic text mapping loading logic shuru karein
        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 3000 // 3 Seconds splash hold time
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val progressValue = animation.animatedValue as Int

            binding.splashProgressBar.progress = progressValue

            when (progressValue) {
                in 0..35 -> binding.tvLoadingStatus.text = "Loading system libraries..."
                in 36..70 -> binding.tvLoadingStatus.text = "Initializing camera engine..."
                in 71..99 -> binding.tvLoadingStatus.text = "Optimizing UI modules..."
                100 -> binding.tvLoadingStatus.text = "Ready!"
            }
        }

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                navigateToNextScreen()
            }
        })

        animator.start()
    }

    private fun navigateToNextScreen() {
        if (!isAdded) return

        // FIXED: PrefManager context extraction system instance cleanly loaded here
        val prefs = PrefManager.get(requireContext())

        when {

            // Case 2: Language select ho chuki hai par onboarding abhi baki hai
            !prefs.isOnboardingDone -> {
                findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)
            }
            // Case 3: Sab completed hai (Returning user dashboard flow redirect)
            else -> {
                (activity as? MainActivity)?.goToMain()

            }
        }
    }
}