package com.futurecode.hdcameramax.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.databinding.ItemGalleryBinding
import com.futurecode.hdcameramax.model.MediaItem

class GalleryAdapter(
    private var items: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit,
    private val onFavouriteClick: (MediaItem) -> Unit = {},
    private val isFavourite: (MediaItem) -> Boolean = { false }
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    inner class GalleryViewHolder(val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val item = items[position]

        Glide.with(holder.itemView.context)
            .load(item.uri)
            .placeholder(R.color.card_border_light)
            .centerCrop()
            .into(holder.binding.ivGalleryThumbnail)

        holder.binding.ivGalleryThumbnail.contentDescription =
            if (item.isVideo) "Video" else "Photo"
        holder.binding.ivVideoBadge.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        holder.binding.ivFavourite.imageTintList = ContextCompat.getColorStateList(
            holder.itemView.context,
            if (isFavourite(item)) R.color.permission_green else R.color.text_gray_dim
        )
        holder.binding.ivFavourite.setOnClickListener {
            onFavouriteClick(item)
        }
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MediaItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
