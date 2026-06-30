package com.futurecode.hdcameramax.adapter

import androidx.core.content.ContextCompat
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.base.BaseAdapter
import com.futurecode.hdcameramax.databinding.ItemResolutionPresetBinding
import com.futurecode.hdcameramax.model.ResolutionPreset

class ResolutionPresetAdapter(
    private val onResolutionClick: (ResolutionPreset) -> Unit
) : BaseAdapter<ResolutionPreset, ItemResolutionPresetBinding>(ItemResolutionPresetBinding::inflate) {

    private var selectedPreset: ResolutionPreset? = null

    override fun bind(binding: ItemResolutionPresetBinding, item: ResolutionPreset, position: Int) {
        val context = binding.root.context
        val selected = item == selectedPreset

        binding.tvResolutionSize.text = item.displayString
        binding.tvResolutionMeta.text = "${item.ratioLabel}  -  ${item.qualityGroup}"
        binding.tvRecommended.visibility = if (item.isRecommended) android.view.View.VISIBLE else android.view.View.GONE
        binding.ivSelected.setImageResource(
            if (selected) R.drawable.bg_selected_check else R.drawable.bg_unselected_radio
        )
        binding.cardResolution.setCardBackgroundColor(
            ContextCompat.getColor(context, if (selected) R.color.permission_green_light else R.color.white)
        )
        binding.cardResolution.strokeColor = ContextCompat.getColor(
            context,
            if (selected) R.color.permission_green else R.color.card_border_light
        )

        binding.root.setOnClickListener {
            selectedPreset = item
            notifyDataSetChanged()
            onResolutionClick(item)
        }
    }

    fun submitResolutions(items: List<ResolutionPreset>, selected: ResolutionPreset?) {
        selectedPreset = selected
        submitList(items)
    }
}
