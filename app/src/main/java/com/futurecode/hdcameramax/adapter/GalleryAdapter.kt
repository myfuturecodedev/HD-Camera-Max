package com.futurecode.hdcameramax.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.hdcameramax.databinding.ItemGalleryBinding
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.model.MediaItem


//class GalleryAdapter(private val items: List<GalleryItem>) :
//    RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
//
//    data class GalleryItem(
//        val isVideo: Boolean = false,
//        val duration: String? = null,
//        val isSelected: Boolean = false
//    )
//
//    class GalleryViewHolder(val binding: ItemGalleryBinding) : RecyclerView.ViewHolder(binding.root)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
//        val binding = ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return GalleryViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
//        val item = items[position]
//        holder.binding.apply {
//            ivPlay.visibility = if (item.isVideo) View.VISIBLE else View.GONE
//            tvDuration.visibility = if (item.isVideo) View.VISIBLE else View.GONE
//            tvDuration.text = item.duration
//            ivSelected.visibility = if (item.isSelected) View.VISIBLE else View.GONE
//        }
//    }
//
//    override fun getItemCount(): Int = items.size
//}



class GalleryAdapter(
    private var items: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit
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

        // Glide engine handles dynamic hardware images/video thumbnails loading asynchronously
        Glide.with(holder.itemView.context)
            .load(item.uri)
            .placeholder(R.color.card_border_light)
            .centerCrop()
            .into(holder.binding.ivMedia) // Make sure you have ivThumbnail in item_gallery.xml

        // Video badge indicator handler setup
        // If layout contains video play icon/overlay visibility indicator:
        // holder.binding.ivVideoPlayIcon.visibility = if (item.isVideo) View.VISIBLE else View.GONE

        //holder.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MediaItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}