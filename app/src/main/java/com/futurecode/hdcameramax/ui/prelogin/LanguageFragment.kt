package com.futurecode.hdcameramax.ui.prelogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentLanguageBinding
import com.futurecode.hdcameramax.utils.PrefManager
import com.futurecode.hdcameramax.model.Language

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private lateinit var languageAdapter: LanguageAdapter
    // Core data model items array
    private val languages = mutableListOf<Language>()
    // ✅ FIXED: Unified items collection holder to stream mixed data nodes (Languages & Ads)
    private val mixedLanguageItems = mutableListOf<Any>()
    private var from = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        from = arguments?.getString("from") ?: ""

        setupLanguages()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupLanguages() {
        val currentLang = PrefManager.get(requireContext()).selectedLanguage

        languages.clear()
        languages.add(Language("English", "en", isDefault = true))
        languages.add(Language("العربية", "ar"))
        languages.add(Language("Deutsch", "de"))
        languages.add(Language("Español", "es"))
        languages.add(Language("Français", "fr"))
        languages.add(Language("Bahasa Indonesia", "in"))
        languages.add(Language("Italiano", "it"))
        languages.add(Language("日本語", "ja"))
        languages.add(Language("한국어", "ko"))

        // Set initial selection states safely
        languages.forEach { it.isSelected = it.code == currentLang }
        if (languages.none { it.isSelected }) {
            languages[0].isSelected = true
        }

        // ====================================================================
        // ✅ ADDED: Building Mixed Items pipeline & Injected Native Ad Token
        // ====================================================================
        mixedLanguageItems.clear()
        languages.forEachIndexed { index, language ->
            mixedLanguageItems.add(language)
            // Automations: Auto inject "AD_UNIT" placeholder token after the second item
            if (index == 1) {
                mixedLanguageItems.add("AD_UNIT")
            }
        }
    }

    private fun setupRecyclerView() {
        // ✅ FIXED: Now passing 'requireActivity()' and the 'mixedLanguageItems' list stream securely
        languageAdapter = LanguageAdapter(requireActivity(), mixedLanguageItems) { selectedLanguage ->
            // Clear selection states inside our core source list data array
            languages.forEach { it.isSelected = false }
            selectedLanguage.isSelected = true

            // Refresh adapters lists to update ticks backgrounds changes instantly
            languageAdapter.notifyDataSetChanged()

            applySelectedLanguage(selectedLanguage)

            // Auto trigger navigation behavior on direct item selection matching design standards
            PrefManager.get(requireContext()).selectedLanguage = selectedLanguage.code
            PrefManager.get(requireContext()).isLanguageSelectedFirstTime = true
            navigateAfterLanguageSelection()
        }

        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = languageAdapter
        }
    }

    private fun applySelectedLanguage(selectedLanguage: Language) {
        MyApplication.applyLanguage(selectedLanguage.code)
    }

    private fun setupClickListeners() {
        binding.btnDone.setOnClickListener {
            val selected = languages.find { it.isSelected }
            selected?.let {
                PrefManager.get(requireContext()).selectedLanguage = it.code
                PrefManager.get(requireContext()).isLanguageSelectedFirstTime = true

                applySelectedLanguage(it)
                navigateAfterLanguageSelection()
            }
        }

//        binding.btnUpgrade.setOnClickListener {
//            // Handle upgrade premium action routing
//        }
    }

    private fun navigateAfterLanguageSelection() {
        if (from == SOURCE_AUTH) {
            findNavController().navigate(R.id.action_languageFragment_to_onboardingFragment)
        } else {
            findNavController().navigateUp()
        }
    }

    private companion object {
        const val SOURCE_AUTH = "auth"
    }
}