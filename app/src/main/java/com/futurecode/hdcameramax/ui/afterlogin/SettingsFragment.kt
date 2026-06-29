package com.futurecode.hdcameramax.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentSettingsBinding

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // Toolbar: Back and Favorite
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show()
        }

        // Section: Camera Settings
        binding.itemResolution.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Resolution Settings", Toast.LENGTH_SHORT).show()
        }

        // Section: General
        binding.itemLanguage.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Language Selection", Toast.LENGTH_SHORT).show()
        }

        // More Options
        binding.itemRateUs.setOnClickListener {
            Toast.makeText(requireContext(), "Redirecting to Play Store...", Toast.LENGTH_SHORT).show()
        }

        binding.itemShareApp.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Share Sheet...", Toast.LENGTH_SHORT).show()
        }

        binding.itemPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Privacy Policy", Toast.LENGTH_SHORT).show()
        }

        binding.itemFeedback.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Feedback Form", Toast.LENGTH_SHORT).show()
        }
    }


}
