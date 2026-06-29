package com.futurecode.hdcameramax.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentPhotoAndVideoViewBinding

class PhotoAndVideoViewFragment : BaseFragment<FragmentPhotoAndVideoViewBinding>(FragmentPhotoAndVideoViewBinding::inflate) {

    private var isFavorite = true



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        updateFavoriteUI()
    }

    private fun setupClickListeners() {
        // Top and Bottom Bar Actions
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnShare.setOnClickListener {
            Toast.makeText(requireContext(), "Share clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteUI()
        }

        binding.btnDelete.setOnClickListener {
            binding.overlayDelete.visibility = View.VISIBLE
        }

        binding.btnMore.setOnClickListener {
            binding.overlayMore.visibility = View.VISIBLE
        }

        // More Menu Overlay
        binding.overlayMore.setOnClickListener {
            binding.overlayMore.visibility = View.GONE
        }

        binding.menuRename.setOnClickListener {
            binding.overlayMore.visibility = View.GONE
            binding.overlayRename.visibility = View.VISIBLE
        }

        binding.menuDownload.setOnClickListener {
            binding.overlayMore.visibility = View.GONE
            Toast.makeText(requireContext(), "Downloading...", Toast.LENGTH_SHORT).show()
        }

        binding.menuWallpaper.setOnClickListener {
            binding.overlayMore.visibility = View.GONE
            Toast.makeText(requireContext(), "Setting as wallpaper...", Toast.LENGTH_SHORT).show()
        }

        binding.menuDetails.setOnClickListener {
            binding.overlayMore.visibility = View.GONE
            Toast.makeText(requireContext(), "Showing details...", Toast.LENGTH_SHORT).show()
        }

        // Delete Dialog
        binding.btnKeepIt.setOnClickListener {
            binding.overlayDelete.visibility = View.GONE
        }

        binding.btnConfirmDelete.setOnClickListener {
            binding.overlayDelete.visibility = View.GONE
            Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.overlayDelete.setOnClickListener {
            binding.overlayDelete.visibility = View.GONE
        }

        // Rename Dialog
        binding.btnCloseRename.setOnClickListener {
            binding.overlayRename.visibility = View.GONE
        }

        binding.btnCancelRename.setOnClickListener {
            binding.overlayRename.visibility = View.GONE
        }

        binding.btnSaveRename.setOnClickListener {
            val newName = binding.etRename.text.toString()
            if (newName.isNotEmpty()) {
                binding.tvFileNameTag.text = "$newName.jpg"
                binding.overlayRename.visibility = View.GONE
                Toast.makeText(requireContext(), "Renamed successfully", Toast.LENGTH_SHORT).show()
            }
        }

        binding.overlayRename.setOnClickListener {
            binding.overlayRename.visibility = View.GONE
        }
    }

    private fun updateFavoriteUI() {
        val color = if (isFavorite) R.color.permission_green else R.color.text_gray_dim
        binding.ivFavorite.imageTintList = ContextCompat.getColorStateList(requireContext(), color)
    }

}
