package com.futurecode.hdcameramax.ui.afterlogin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.databinding.ItemFavouriteBinding
import com.futurecode.hdcameramax.model.MediaItem

class FavouriteAdapter(
    private var items: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit,
    private val onRemoveFavourite: (MediaItem) -> Unit
) :
    RecyclerView.Adapter<FavouriteAdapter.FavouriteViewHolder>() {

    class FavouriteViewHolder(val binding: ItemFavouriteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = ItemFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            Glide.with(root.context)
                .load(item.uri)
                .placeholder(R.drawable.bg_dashboard_recent_placeholder)
                .centerCrop()
                .into(ivThumbnail)

            ivPlay.visibility = if (item.isVideo) View.VISIBLE else View.GONE
            tvDuration.visibility = if (item.isVideo) View.VISIBLE else View.GONE
            tvDuration.text = if (item.isVideo) "00:00" else null
            ivHeart.setOnClickListener {
                onRemoveFavourite(item)
            }
            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MediaItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
