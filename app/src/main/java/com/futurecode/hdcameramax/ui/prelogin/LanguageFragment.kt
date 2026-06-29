package com.futurecode.hdcameramax.ui.prelogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentLanguageBinding
import com.futurecode.hdcameramax.utils.PrefManager
import com.futurecode.hdcameramax.model.Language

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private lateinit var languageAdapter: LanguageAdapter
    // FIXED: Element type standard data model 'Language' hona chahiye, na ki Adapter class
    private val languages = mutableListOf<Language>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLanguages()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupLanguages() {
        val currentLang = PrefManager.get(requireContext()).selectedLanguage

        languages.clear()
        // FIXED: Ek standard default entry rakhi hai duplicate hatane ke liye
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
    }

    private fun setupRecyclerView() {
        languageAdapter = LanguageAdapter(languages) { selectedLanguage ->
            languages.forEach { it.isSelected = false }
            selectedLanguage.isSelected = true
            languageAdapter.notifyDataSetChanged()
        }

        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = languageAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnDone.setOnClickListener {
            val selected = languages.find { it.isSelected }
            selected?.let {
                PrefManager.get(requireContext()).selectedLanguage = it.code
                PrefManager.get(requireContext()).isLanguageSelectedFirstTime = true
                // Navigate to next screen layout flow
                findNavController().navigate(R.id.action_languageFragment_to_permissionFragment)
            }
        }

        binding.btnUpgrade.setOnClickListener {
            // Handle upgrade premium action routing
        }
    }
}