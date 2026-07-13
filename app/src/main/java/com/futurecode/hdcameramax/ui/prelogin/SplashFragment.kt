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
import com.futurecode.hdcameramax.utils.JsonReadUtils
import com.futurecode.hdcameramax.utils.PrefManager

class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val skipSplash = requireActivity().intent.getBooleanExtra("skip_splash", false)

        // API hit karenge, aur response milne par hi direct navigate karwayenge
        JsonReadUtils.fetchJsonData(requireContext()) {
            if (isAdded) {
                //(activity as? MainActivity)?.goToMain()
                 navigateToNextScreen()
            }
        }
    }


    private fun navigateToNextScreen() {

        if (!isAdded) return
        findNavController().navigate(
            R.id.action_splashFragment_to_languageFragment,
            Bundle().apply { putString("from", "auth") }
        )

    }
}