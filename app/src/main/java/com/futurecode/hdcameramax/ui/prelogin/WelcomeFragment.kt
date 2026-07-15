package com.futurecode.hdcameramax.ui.prelogin
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MainActivity
import com.futurecode.hdcameramax.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentWelcomeBinding
import com.futurecode.hdcameramax.utils.Utils.setAdClickListener

class WelcomeFragment : BaseFragment<FragmentWelcomeBinding>(FragmentWelcomeBinding::inflate) {

    private var nativeAdsHelper: NativeAdsHelper? = null
    private var fullScreenAdsHelper: FullScreenAdsHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nativeAdsHelper= NativeAdsHelper(requireActivity())
        fullScreenAdsHelper= FullScreenAdsHelper(requireActivity())

        binding.btnWelcomeBackArrow.bringToFront()
        binding.btnWelcomeBackArrow.translationZ = resources.displayMetrics.density * 8


        // 1. Back button navigation callback mapping
        binding.btnWelcomeBackArrow.setOnClickListener {
            val navController = findNavController()
            if (!navController.popBackStack()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        // 2. Action Continue Routing trigger click listener
        binding.btnWelcomeContinue.setOnClickListener {
            navigateToMainCameraDashboard()
        }


        fullScreenAdsHelper?.let { helper ->
            binding.btnWelcomeContinue.setAdClickListener(
                activity = requireActivity(),
                adsHelper = helper, // ✅ FIXED: Safe non-null reference inside 'let' scope
                isShowEveryTime = false
            ) {
                navigateToMainCameraDashboard()
            }
        }


    }

    private fun navigateToMainCameraDashboard() {
        if (!isAdded) return

       // findNavController().navigate(R.id.action_welcomeFragment_to_languageFragment)

        // val intent = android.content.Intent(requireActivity(), HomeActivity::class.java)
        // startActivity(intent)
        // requireActivity().finish()

        (activity as? MainActivity)?.goToMain()
    }
}
