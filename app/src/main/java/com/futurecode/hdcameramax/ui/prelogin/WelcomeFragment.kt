package com.futurecode.hdcameramax.ui.prelogin
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentWelcomeBinding
class WelcomeFragment : BaseFragment<FragmentWelcomeBinding>(FragmentWelcomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Back button navigation callback mapping
        binding.btnWelcomeBackArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Action Continue Routing trigger click listener
        binding.btnWelcomeContinue.setOnClickListener {
            navigateToMainCameraDashboard()
        }
    }

    private fun navigateToMainCameraDashboard() {
        if (!isAdded) return

        findNavController().navigate(R.id.action_welcomeFragment_to_languageFragment)

        // val intent = android.content.Intent(requireActivity(), HomeActivity::class.java)
        // startActivity(intent)
        // requireActivity().finish()
    }
}