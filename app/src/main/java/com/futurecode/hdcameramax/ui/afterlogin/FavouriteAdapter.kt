package com.futurecode.hdcameramax.ui.afterlogin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.hdcameramax.databinding.ItemFavouriteBinding

class FavouriteAdapter(private val items: List<FavouriteItem>) :
    RecyclerView.Adapter<FavouriteAdapter.FavouriteViewHolder>() {

    data class FavouriteItem(
        val isVideo: Boolean = false,
        val duration: String? = null
    )

    class FavouriteViewHolder(val binding: ItemFavouriteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = ItemFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            ivPlay.visibility = if (item.isVideo) View.VISIBLE else View.GONE
            tvDuration.visibility = if (item.isVideo) View.VISIBLE else View.GONE
            tvDuration.text = item.duration
        }
    }

    override fun getItemCount(): Int = items.size
}