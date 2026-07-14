package com.futurecode.hdcameramax.ui.afterlogin

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.futurecode.hdcameramax.BuildConfig
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.adapter.ResolutionPresetAdapter
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.DialogResolutionSelectorBinding
import com.futurecode.hdcameramax.databinding.FragmentSettingsBinding
import com.futurecode.hdcameramax.model.ResolutionPreset
import com.google.android.material.bottomsheet.BottomSheetDialog

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private lateinit var viewModel: HdCameraViewModel


    private lateinit var nativeAdsHelper: NativeAdsHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            HdCameraViewModel.Factory(
                HdCameraRepository(requireContext().applicationContext)
            )
        )[HdCameraViewModel::class.java]

        setupListeners()
        observeResolutionState()
        nativeAdsHelper= NativeAdsHelper(requireActivity())


        binding.appVersion.text = "Version: ${BuildConfig.VERSION_NAME}"

        loadNativeAds()
    }

    private fun setupListeners() {
        // Toolbar: Back and Favorite
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_favouriteFragment)
        }

        // Section: Camera Settings
        binding.itemResolution.setOnClickListener {
            showResolutionSelector()
        }

        // Section: General
        binding.itemLanguage.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_languageFragment2,
                Bundle().apply { putString("from", SOURCE_SETTINGS) }
            )
        }

        // More Options
        binding.itemRateUs.setOnClickListener {
            val uri = Uri.parse("market://details?id=${requireContext().packageName}")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(goToMarket)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")))
            }
        }

        binding.itemShareApp.setOnClickListener {
            // 1. Text payload build kijiye jisme app ka Play Store dynamic link reference automatic bundled ho
            val appPackageName = requireContext().packageName
            val shareBodyText = "Check out HD Camera Max! Capture stunning moments with enhanced zoom and professional filters. Download now:\nhttps://play.google.com/store/apps/details?id=$appPackageName"

            // 2. Instantiate core transport intent engine
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain" // Sets strict text data standard format definition
                putExtra(android.content.Intent.EXTRA_SUBJECT, "HD Camera Max App Share")
                putExtra(android.content.Intent.EXTRA_TEXT, shareBodyText)
            }

            try {
                // 3. Wrap inside a clean system-native chooser layout sheet safely
                startActivity(android.content.Intent.createChooser(shareIntent, "Share App Via"))
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    requireContext(),
                    "Unable to open sharing application right now.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.itemPrivacy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(prefManager.privacyPolicy ?: "")) // Replace with actual URL
            startActivity(browserIntent)
        }

        binding.itemFeedback.setOnClickListener {
            val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                // Sets data schema uri to strictly force only mailing applications to intercept the action
                data = android.net.Uri.parse("mailto:")

                // Target recipient email address matching your publisher console guidelines
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@futurecode.com"))

                // Automated subject formatting template to track application version streams easily
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback & Suggestions: HD Camera Max")

                // Core diagnostic body layout string text
                putExtra(android.content.Intent.EXTRA_TEXT, "\n\n\n---\nDevice Details:\nModel: ${android.os.Build.MODEL}\nOS Version: Android ${android.os.Build.VERSION.RELEASE}")
            }

            try {
                // Launches user preferred mailing client context cleanly
                startActivity(emailIntent)
            } catch (e: android.content.ActivityNotFoundException) {
                // Fallback protection handler loop in case the target testing device contains no active email apps
                android.widget.Toast.makeText(
                    requireContext(),
                    "No email application found on this device.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showResolutionSelector() {
        val dialogBinding = DialogResolutionSelectorBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        val adapter = ResolutionPresetAdapter { preset ->
            applyResolutionPreset(preset)
            dialog.dismiss()
        }
        val state = viewModel.uiState.value ?: HdCameraUiState()

        dialogBinding.rvResolutions.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvResolutions.adapter = adapter
        fun renderFilterSelection(selected: TextView) {
            listOf(
                dialogBinding.btnRatioAll,
                dialogBinding.btnRatio43,
                dialogBinding.btnRatio169,
                dialogBinding.btnRatio11,
                dialogBinding.btnRatioRecommended
            )
                .forEach { chip ->
                    val active = chip == selected
                    chip.setBackgroundResource(
                        if (active) R.drawable.bg_camera_mode_active else R.drawable.bg_camera_resolution_filter
                    )
                    chip.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (active) R.color.white else R.color.text_dark_primary
                        )
                    )
                    chip.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
                }
        }

        fun submitFilteredResolutions(filter: String?) {
            val latestState = viewModel.uiState.value ?: state
            val presets = latestState.resolutionPresets.ifEmpty {
                HdCameraRepository(requireContext().applicationContext).getResolutionPresets()
            }
            val filtered = when (filter) {
                null -> presets
                "Recommended" -> presets.filter { it.isRecommended }
                else -> presets.filter { it.ratioLabel == filter }
            }
            adapter.submitResolutions(filtered, latestState.selectedResolution ?: presets.firstOrNull())
        }

        renderFilterSelection(dialogBinding.btnRatioAll)
        submitFilteredResolutions(null)
        dialogBinding.btnCloseDialog.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnRatioAll.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatioAll)
            submitFilteredResolutions(null)
        }
        dialogBinding.btnRatio43.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatio43)
            submitFilteredResolutions("4:3")
        }
        dialogBinding.btnRatio169.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatio169)
            submitFilteredResolutions("16:9")
        }
        dialogBinding.btnRatio11.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatio11)
            submitFilteredResolutions("1:1")
        }
        dialogBinding.btnRatioRecommended.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatioRecommended)
            submitFilteredResolutions("Recommended")
        }

        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun applyResolutionPreset(preset: ResolutionPreset) {
        viewModel.selectResolution(preset)
        binding.tvResolutionValue.text = preset.displayString
        // cameraKit.setManualResolution(preset.width, preset.height)
    }

    private fun observeResolutionState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val selectedResolution = state.selectedResolution
                ?: state.resolutionPresets.firstOrNull()
            binding.tvResolutionValue.text = selectedResolution?.displayString
                ?: getString(R.string.resolution_value)
        }
    }


    fun loadNativeAds() {
        activity?.let { currentActivity ->
            if (nativeAdsHelper == null) {
                nativeAdsHelper = NativeAdsHelper(currentActivity)
            }
            nativeAdsHelper?.showNativeAd(
                nativeBannerAdView = binding.nativeAds3.frame,
                mainLayout = binding.nativeAds3.mainLayout,
                placeholder = binding.nativeAds3.placeholder
            )
        }
    }
    private companion object {
        const val SOURCE_SETTINGS = "settings"
    }
}
