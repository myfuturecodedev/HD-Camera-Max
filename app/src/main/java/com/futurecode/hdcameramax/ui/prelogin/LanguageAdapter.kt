package com.futurecode.hdcameramax.ui.prelogin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.databinding.ItemLanguageBinding
import com.futurecode.hdcameramax.model.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    class LanguageViewHolder(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        holder.binding.apply {
            tvLanguageName.text = if (language.isDefault) "${language.name} (default)" else language.name

            if (language.isSelected) {
                cardLanguage.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.permission_green_light))
                cardLanguage.strokeColor = ContextCompat.getColor(root.context, R.color.permission_green)
                ivSelection.setImageResource(R.drawable.bg_selected_check)
                ivSelection.imageTintList = null
            } else {
                cardLanguage.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.white))
                cardLanguage.strokeColor = ContextCompat.getColor(root.context, R.color.card_border_light)
                ivSelection.setImageResource(R.drawable.bg_circle_icon)
                ivSelection.imageTintList = ContextCompat.getColorStateList(root.context, R.color.card_border_light)
            }

            root.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }

    override fun getItemCount(): Int = languages.size
}