package com.futurecode.hdcameramax.ui.afterlogin

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentPhotoAndVideoViewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoAndVideoViewFragment :
    BaseFragment<FragmentPhotoAndVideoViewBinding>(FragmentPhotoAndVideoViewBinding::inflate) {

    private var selectedMediaUri: Uri? = null
    private var selectedDisplayName: String = ""
    private var selectedIsVideo: Boolean = false
    private var isFavorite = false
    private lateinit var favouriteRepository: FavouriteRepository
    private val videoProgressHandler = Handler(Looper.getMainLooper())
    private var shouldLoopVideo = false

    private val videoProgressRunnable = object : Runnable {
        override fun run() {
            updateVideoProgress()
            videoProgressHandler.postDelayed(this, VIDEO_PROGRESS_INTERVAL_MS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favouriteRepository = FavouriteRepository(requireContext())
        selectedMediaUri = arguments?.getString(ARG_MEDIA_URI)?.let(Uri::parse)
        selectedIsVideo = arguments?.getBoolean(ARG_IS_VIDEO) ?: false
        selectedDisplayName = selectedMediaUri?.let { loadDisplayName(it) }.orEmpty()
        isFavorite = selectedMediaUri?.let { favouriteRepository.isFavourite(it) } == true

        renderSelectedMedia()
        renderMediaMetadata()
        setupClickListeners()
        updateFavoriteUI()
    }

    override fun onDestroyView() {
        videoProgressHandler.removeCallbacks(videoProgressRunnable)
        bindingOrNull?.videoViewPlayer?.stopPlayback()
        super.onDestroyView()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnShare.setOnClickListener {
            shareSelectedMedia()
        }

        binding.btnFavorite.setOnClickListener {
            val mediaUri = selectedMediaUri ?: return@setOnClickListener
            isFavorite = favouriteRepository.toggleFavourite(mediaUri)
            updateFavoriteUI()
        }

        binding.btnDelete.setOnClickListener { showDeleteConfirmationDialog() }

        // FIXED PIPELINE: Trigger native anchor coordinates injection instead of a full screen window dialog
        binding.btnMore.setOnClickListener {
            showMoreOptionsPopupWindow(it) // 'it' means btnMore active anchor view reference
        }

        binding.btnVideoBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnVideoFavorite.setOnClickListener {
            val mediaUri = selectedMediaUri ?: return@setOnClickListener
            isFavorite = favouriteRepository.toggleFavourite(mediaUri)
            updateFavoriteUI()
        }

        binding.btnVideoCenterPlay.setOnClickListener { toggleVideoPlayback() }
        binding.btnVideoPlayPause.setOnClickListener { toggleVideoPlayback() }

        binding.btnVideoPrevious.setOnClickListener {
            seekVideoBy(-VIDEO_SEEK_STEP_MS)
        }

        binding.btnVideoNext.setOnClickListener {
            seekVideoBy(VIDEO_SEEK_STEP_MS)
        }

        binding.btnVideoRepeat.setOnClickListener {
            shouldLoopVideo = !shouldLoopVideo
            binding.btnVideoRepeat.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (shouldLoopVideo) R.color.permission_green else R.color.white
                )
            )
        }

        binding.btnVideoFullscreen.setOnClickListener {
            Toast.makeText(requireContext(), "Fullscreen", Toast.LENGTH_SHORT).show()
        }

        binding.btnVideoLock.setOnClickListener {
            Toast.makeText(requireContext(), "Controls locked", Toast.LENGTH_SHORT).show()
        }

        binding.sbVideoProgress.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val duration = binding.videoViewPlayer.duration.coerceAtLeast(0)
                        val target = (duration * (progress / VIDEO_PROGRESS_MAX.toFloat())).toInt()
                        binding.videoViewPlayer.seekTo(target)
                        binding.tvVideoElapsed.text = formatPlaybackTime(target)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            }
        )
    }

    private fun updateFavoriteUI() {
        val color = if (isFavorite) R.color.permission_green else R.color.text_gray_dim
        binding.ivFavorite.imageTintList = ContextCompat.getColorStateList(requireContext(), color)
        binding.btnVideoFavorite.imageTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (isFavorite) R.color.permission_green else R.color.white
        )
    }

    // ==========================================
    // UPGRADED MODULE 1: Precise Anchor PopupWindow Engine
    // ==========================================
// ==========================================
    // FIXED: Precise Real-time Anchor PopupWindow Calculation Engine
    // ==========================================
// ==========================================
    // FINAL FIXED MODULE: Strict Height-Locked Floating Popup Window
    // ==========================================
    private fun showMoreOptionsPopupWindow(anchorView: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.dialog_more_options, null)

        // Force manual specification to explicitly prevent layout expansion
        val exactWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._220sdp)

        // Exact measure specs force rules applied
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(exactWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val exactHeight = popupView.measuredHeight

        // Instantiate popup with STRICT pixel boundary specifications instead of generic Wrap/Match declarations
        val popupWindow = PopupWindow(
            popupView,
            exactWidth,
            exactHeight,
            true
        )

        // Clear layout hardware caching blocks window variables hooks
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        // Bind inner rows action buttons listeners safely
        popupView.findViewById<View>(R.id.rowRename).setOnClickListener {
            popupWindow.dismiss()
            showRenameOverlayDialog()
        }
        popupView.findViewById<View>(R.id.rowDownload).setOnClickListener {
            popupWindow.dismiss()
            Toast.makeText(requireContext(), "Already saved in CameraHDMax", Toast.LENGTH_SHORT).show()
        }
        popupView.findViewById<View>(R.id.rowWallpaper).setOnClickListener {
            popupWindow.dismiss()
            val message = if (selectedIsVideo) {
                "Wallpaper is available for photos only"
            } else {
                "Open the system gallery to set wallpaper"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        popupView.findViewById<View>(R.id.rowDetails).setOnClickListener {
            popupWindow.dismiss()
            showMediaDetails()
        }

        // --- MATH RE-CALCULATION FOR ACCURATE ANCHOR POSITION ---
        val xOffset = -(exactWidth - anchorView.width)

        // Moving menu exactly over the target anchor without pushing down screen bounds parameters
        val spacingOffset = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
        val yOffset = -(exactHeight + anchorView.height + spacingOffset)

        // Execute precise window coordinate display loops
        popupWindow.showAsDropDown(anchorView, xOffset, yOffset)
    }

    private fun showDeleteConfirmationDialog() {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_delete_media)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }

        dialog.findViewById<View>(R.id.btnActionKeepIt).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnActionConfirmDelete).setOnClickListener {
            dialog.dismiss()
            deleteSelectedMedia()
        }
        dialog.show()
    }

    private fun showRenameOverlayDialog() {
        binding.overlayRename.visibility = View.VISIBLE
        binding.btnCloseRename.setOnClickListener { binding.overlayRename.visibility = View.GONE }
        binding.btnCancelRename.setOnClickListener { binding.overlayRename.visibility = View.GONE }
        binding.overlayRename.setOnClickListener { binding.overlayRename.visibility = View.GONE }

        binding.btnSaveRename.setOnClickListener {
            val newName = binding.etRename.text.toString().trim()
            if (newName.isNotEmpty()) {
                renameSelectedMedia(newName)
                binding.overlayRename.visibility = View.GONE
            }
        }
    }

    private fun renderSelectedMedia() {
        val mediaUri = selectedMediaUri
        binding.videoPlayerRoot.visibility = if (selectedIsVideo) View.VISIBLE else View.GONE
        binding.topBar.visibility = if (selectedIsVideo) View.GONE else View.VISIBLE
        binding.tvFileNameTag.visibility = if (selectedIsVideo) View.GONE else View.VISIBLE
        binding.ivMain.visibility = if (selectedIsVideo) View.GONE else View.VISIBLE
        binding.bottomBar.visibility = if (selectedIsVideo) View.GONE else View.VISIBLE

        if (mediaUri == null) {
            binding.ivMain.setImageResource(R.drawable.native_thumb)
            return
        }

        if (selectedIsVideo) {
            renderVideoMedia(mediaUri)
            return
        }

        Glide.with(this)
            .load(mediaUri)
            .placeholder(R.drawable.native_thumb)
            .fitCenter()
            .into(binding.ivMain)

        if (selectedDisplayName.isBlank()) {
            selectedDisplayName = if (selectedIsVideo) "Video.mp4" else "Photo.jpg"
        }
        binding.tvFileNameTag.text = selectedDisplayName
    }

    private fun renderVideoMedia(mediaUri: Uri) {
        Glide.with(this)
            .load(mediaUri)
            .placeholder(R.drawable.native_thumb)
            .centerCrop()
            .into(binding.ivVideoPoster)

        if (selectedDisplayName.isBlank()) {
            selectedDisplayName = "Video.mp4"
        }
        binding.tvVideoTitle.text = selectedDisplayName.substringBeforeLast('.', selectedDisplayName)
        binding.tvVideoFileName.text = selectedDisplayName
        binding.videoViewPlayer.setVideoURI(mediaUri)
        binding.videoViewPlayer.setOnPreparedListener { player ->
            binding.tvVideoDuration.text = formatPlaybackTime(player.duration)
            binding.tvVideoElapsed.text = formatPlaybackTime(0)
            binding.sbVideoProgress.max = VIDEO_PROGRESS_MAX
            binding.sbVideoProgress.progress = 0
            player.isLooping = shouldLoopVideo
            binding.ivVideoPoster.visibility = View.GONE
            binding.videoViewPlayer.start()
            renderVideoPlaybackState(isPlaying = true)
            videoProgressHandler.removeCallbacks(videoProgressRunnable)
            videoProgressHandler.post(videoProgressRunnable)
        }
        binding.videoViewPlayer.setOnCompletionListener {
            if (shouldLoopVideo) {
                binding.videoViewPlayer.start()
                renderVideoPlaybackState(isPlaying = true)
            } else {
                renderVideoPlaybackState(isPlaying = false)
                binding.sbVideoProgress.progress = VIDEO_PROGRESS_MAX
            }
        }
    }

    private fun renderMediaMetadata() {
        val createdAt = arguments?.getLong(ARG_DATE_ADDED)?.takeIf { it > 0L } ?: System.currentTimeMillis()
        binding.textDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdAt))
        binding.time.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(createdAt))
    }

    private fun toggleVideoPlayback() {
        if (!selectedIsVideo) return
        if (binding.videoViewPlayer.isPlaying) {
            binding.videoViewPlayer.pause()
            renderVideoPlaybackState(isPlaying = false)
        } else {
            binding.ivVideoPoster.visibility = View.GONE
            binding.videoViewPlayer.start()
            renderVideoPlaybackState(isPlaying = true)
            videoProgressHandler.removeCallbacks(videoProgressRunnable)
            videoProgressHandler.post(videoProgressRunnable)
        }
    }

    private fun renderVideoPlaybackState(isPlaying: Boolean) {
        val label = if (isPlaying) "II" else ">"
        binding.btnVideoCenterPlay.text = label
        binding.btnVideoPlayPause.text = label
    }

    private fun seekVideoBy(deltaMs: Int) {
        val duration = binding.videoViewPlayer.duration.coerceAtLeast(0)
        if (duration == 0) return
        val target = (binding.videoViewPlayer.currentPosition + deltaMs).coerceIn(0, duration)
        binding.videoViewPlayer.seekTo(target)
        updateVideoProgress()
    }

    private fun updateVideoProgress() {
        if (!selectedIsVideo || bindingOrNull == null) return
        val duration = binding.videoViewPlayer.duration.coerceAtLeast(0)
        val current = binding.videoViewPlayer.currentPosition.coerceAtLeast(0)
        binding.tvVideoElapsed.text = formatPlaybackTime(current)
        binding.tvVideoDuration.text = formatPlaybackTime(duration)
        binding.sbVideoProgress.progress = if (duration > 0) {
            ((current / duration.toFloat()) * VIDEO_PROGRESS_MAX).toInt().coerceIn(0, VIDEO_PROGRESS_MAX)
        } else {
            0
        }
        if (!binding.videoViewPlayer.isPlaying) {
            videoProgressHandler.removeCallbacks(videoProgressRunnable)
        }
    }

    private fun formatPlaybackTime(milliseconds: Int): String {
        val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun shareSelectedMedia() {
        val mediaUri = selectedMediaUri ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (selectedIsVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, mediaUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)))
    }

    private fun deleteSelectedMedia() {
        val mediaUri = selectedMediaUri ?: return
        val deleted = runCatching {
            requireContext().contentResolver.delete(mediaUri, null, null)
        }.getOrDefault(0)

        if (deleted > 0) {
            favouriteRepository.removeFavourite(mediaUri)
            Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } else {
            Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renameSelectedMedia(newBaseName: String) {
        val mediaUri = selectedMediaUri ?: return
        val cleanBaseName = newBaseName.substringBeforeLast('.').trim()
        if (cleanBaseName.isEmpty()) return

        val currentName = selectedDisplayName.ifBlank {
            if (selectedIsVideo) "Video.mp4" else "Photo.jpg"
        }
        val extension = currentName.substringAfterLast(
            delimiter = ".",
            missingDelimiterValue = if (selectedIsVideo) "mp4" else "jpg"
        )
        val newDisplayName = "$cleanBaseName.$extension"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
        }

        val updated = runCatching {
            requireContext().contentResolver.update(mediaUri, values, null, null)
        }.getOrDefault(0)

        if (updated > 0) {
            selectedDisplayName = newDisplayName
            binding.tvFileNameTag.text = newDisplayName
            Toast.makeText(requireContext(), "Renamed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Rename failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMediaDetails() {
        val type = if (selectedIsVideo) "Video" else "Photo"
        val createdAt = arguments?.getLong(ARG_DATE_ADDED)?.takeIf { it > 0L }?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
        } ?: "Unknown date"
        Toast.makeText(
            requireContext(),
            "$type • ${selectedDisplayName.ifBlank { "CameraHDMax media" }} • $createdAt",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun loadDisplayName(uri: Uri): String? {
        return runCatching {
            requireContext().contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            }
        }.getOrNull()
    }

    companion object {
        const val ARG_MEDIA_URI = "media_uri"
        const val ARG_IS_VIDEO = "is_video"
        const val ARG_DATE_ADDED = "date_added"
        const val VIDEO_PROGRESS_MAX = 1000
        const val VIDEO_SEEK_STEP_MS = 10_000
        const val VIDEO_PROGRESS_INTERVAL_MS = 500L
    }
}
