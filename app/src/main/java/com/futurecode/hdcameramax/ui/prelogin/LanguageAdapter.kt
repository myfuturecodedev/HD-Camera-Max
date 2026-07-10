package com.futurecode.hdcameramax.ui.prelogin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.databinding.ItemLanguageBinding
import com.futurecode.hdcameramax.model.Language
import android.app.Activity
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.databinding.ItemNativeAdsAdapterBinding

//class LanguageAdapter(
//    private val languages: List<Language>,
//    private val onLanguageSelected: (Language) -> Unit
//) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {
//
//    class LanguageViewHolder(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
//        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return LanguageViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
//        val language = languages[position]
//        holder.binding.apply {
//            tvLanguageName.text = if (language.isDefault) "${language.name} (default)" else language.name
//
//            if (language.isSelected) {
//                cardLanguage.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.permission_green_light))
//                cardLanguage.strokeColor = ContextCompat.getColor(root.context, R.color.permission_green)
//                ivSelection.setImageResource(R.drawable.bg_selected_check)
//                ivSelection.imageTintList = null
//            } else {
//                cardLanguage.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.white))
//                cardLanguage.strokeColor = ContextCompat.getColor(root.context, R.color.card_border_light)
//                ivSelection.setImageResource(R.drawable.bg_circle_icon)
//                ivSelection.imageTintList = ContextCompat.getColorStateList(root.context, R.color.card_border_light)
//            }
//
//            root.setOnClickListener {
//                onLanguageSelected(language)
//            }
//        }
//    }
//
//    override fun getItemCount(): Int = languages.size
//}






// Helper Constants to safe guard rendering layout view types
object AdViewTypeManager {
    const val TYPE_ITEM = 0
    const val TYPE_AD = 1
}

class LanguageAdapter(
    private val activity: Activity,
    private val list: List<Any>, // ✅ Changed to List<Any> to support both Language models & "AD_UNIT" Strings
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        val item = list[position]
        // ✅ FIXED SYNC: Identifies ad placeholder tokens inside the unified list pipeline
        return if (item is String && item == "AD_UNIT") {
            AdViewTypeManager.TYPE_AD
        } else {
            AdViewTypeManager.TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == AdViewTypeManager.TYPE_AD) {
            val adBinding = ItemNativeAdsAdapterBinding.inflate(inflater, parent, false)
            AdViewHolder(adBinding)
        } else {
            val binding = ItemLanguageBinding.inflate(inflater, parent, false)
            LanguageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        if (holder is LanguageViewHolder && item is Language) {
            holder.bind(item)
        } else if (holder is AdViewHolder) {
            holder.bindAd()
        }
    }

    override fun getItemCount(): Int = list.size

    // ====================================================================
    // CONTENT HOLDER: Handles exact Figma item formatting rules flawlessly
    // ====================================================================
    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(language: Language) {
            binding.apply {
                // Formatting text dynamically matching default fallback locales
                tvLanguageName.text = if (language.isDefault) "${language.name} (default)" else language.name

                // UI rendering changes depending on activation/selection ticks
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
    }

    // ====================================================================
    // NATIVE AD HOLDER: Handles automated injection calls via reference engine
    // ====================================================================
    inner class AdViewHolder(private val binding: ItemNativeAdsAdapterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindAd() {
            // Invokes the native ad framework helper onto item layout references smoothly
            NativeAdsHelper(activity).showNativeAd(
                binding.frameLayout,
                binding.relativeLayout,
                binding.placeholder
            )
        }
    }
}