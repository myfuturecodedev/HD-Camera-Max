package com.futurecode.hdcameramax.adapter

import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.hdcameramax.databinding.ItemOnboardingSlideBinding
import com.futurecode.hdcameramax.model.OnboardingSlide

class OnboardingAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<OnboardingAdapter.SliderViewHolder>() {

    inner class SliderViewHolder(val binding: ItemOnboardingSlideBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val binding = ItemOnboardingSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val slide = slides[position]
        holder.binding.apply {
            ivOnboardingArt.setImageResource(slide.imageRes)
            tvOnboardingTitle.text = Html.fromHtml(slide.title, Html.FROM_HTML_MODE_COMPACT)
            tvOnboardingDesc.text = slide.description
        }
    }

    override fun getItemCount(): Int = slides.size
}