package com.futurecode.hdcameramax.ui.afterlogin

import android.app.Dialog
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.DialogApplyingWallpaperBinding
import com.futurecode.hdcameramax.databinding.DialogSetWallpaperBinding
import com.futurecode.hdcameramax.databinding.FragmentPhotoAndVideoViewBinding
import com.futurecode.hdcameramax.utils.Utils.setAdClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var nativeAdsHelper: NativeAdsHelper? = null
    private var fullScreenAdsHelper: FullScreenAdsHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nativeAdsHelper= NativeAdsHelper(requireActivity())
        fullScreenAdsHelper= FullScreenAdsHelper(requireActivity())

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


        fullScreenAdsHelper?.let { helper ->
            binding.btnShare.setAdClickListener(
                activity = requireActivity(),
                adsHelper = helper, // ✅ FIXED: Safe non-null reference inside 'let' scope
                isShowEveryTime = false
            ) {
                shareSelectedMedia()
            }
        }

        fullScreenAdsHelper?.let { helper ->
            binding.btnFavorite.setAdClickListener(
                activity = requireActivity(),
                adsHelper = helper, // ✅ FIXED: Safe non-null reference inside 'let' scope
                isShowEveryTime = false
            ) {
                val mediaUri = selectedMediaUri ?: return@setAdClickListener
                isFavorite = favouriteRepository.toggleFavourite(mediaUri)
                updateFavoriteUI()
            }
        }



        fullScreenAdsHelper?.let { helper ->
            binding.btnDelete.setAdClickListener(
                activity = requireActivity(),
                adsHelper = helper, // ✅ FIXED: Safe non-null reference inside 'let' scope
                isShowEveryTime = false
            ) {
                showDeleteConfirmationDialog()
            }
        }

        // FIXED PIPELINE: Trigger native anchor coordinates injection instead of a full screen window dialog
        binding.btnMore.setOnClickListener {
            showMoreOptionsPopupWindow(it) // 'it' means btnMore active anchor view reference
        }


//        fullScreenAdsHelper?.let { helper ->
//            binding.btnShare.setAdClickListener(
//                activity = requireActivity(),
//                adsHelper = helper, // ✅ FIXED: Safe non-null reference inside 'let' scope
//                isShowEveryTime = false
//            ) {
//                showMoreOptionsPopupWindow(it) // 'it' means btnMore active anchor view reference
//
//            }
//        }

        binding.btnVideoBack.setOnClickListener { findNavController().navigateUp() }

//        binding.btnVideoFavorite.setOnClickListener {
//            val mediaUri = selectedMediaUri ?: return@setOnClickListener
//            isFavorite = favouriteRepository.toggleFavourite(mediaUri)
//            updateFavoriteUI()
//        }



        fullScreenAdsHelper?.let { helper ->
            binding.btnVideoFavorite.setAdClickListener(
                activity = requireActivity(),
                adsHelper = helper, // ✅ FIXED: Safe non-null reference inside 'let' scope
                isShowEveryTime = false
            ) {
                val mediaUri = selectedMediaUri ?: return@setAdClickListener
                isFavorite = favouriteRepository.toggleFavourite(mediaUri)
                updateFavoriteUI()
            }
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
            if (selectedIsVideo) {
                Toast.makeText(
                    requireContext(),
                    "Wallpaper is available for photos only",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showSetWallpaperDialog()
            }
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

    private fun showSetWallpaperDialog() {
        val mediaUri = selectedMediaUri ?: run {
            Toast.makeText(requireContext(), "Photo not found", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogSetWallpaperBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCancelable(true)
            show()
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        Glide.with(this)
            .load(mediaUri)
            .placeholder(R.drawable.native_thumb)
            .centerCrop()
            .into(dialogBinding.ivWallpaperPreview)

        var selectedTarget = WALLPAPER_TARGET_HOME

        fun renderTargetSelection(target: Int) {
            selectedTarget = target
            val rows = listOf(
                WallpaperTargetViews(
                    dialogBinding.rowHomeScreen,
                    dialogBinding.tvHomeIcon,
                    dialogBinding.tvHomeCheck,
                    WALLPAPER_TARGET_HOME
                ),
                WallpaperTargetViews(
                    dialogBinding.rowLockScreen,
                    dialogBinding.tvLockIcon,
                    dialogBinding.tvLockCheck,
                    WALLPAPER_TARGET_LOCK
                ),
                WallpaperTargetViews(
                    dialogBinding.rowHomeLockScreen,
                    dialogBinding.tvBothIcon,
                    dialogBinding.tvBothCheck,
                    WALLPAPER_TARGET_BOTH
                )
            )

            rows.forEach { item ->
                val selected = item.target == target
                item.row.setBackgroundResource(
                    if (selected) R.drawable.bg_wallpaper_option_selected else R.drawable.bg_wallpaper_option_inactive
                )
                item.icon.setBackgroundResource(
                    if (selected) R.drawable.bg_wallpaper_option_icon_selected else R.drawable.bg_wallpaper_option_icon_inactive
                )
                item.icon.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (selected) R.color.permission_green else R.color.text_gray_dim
                    )
                )
                item.check.visibility = if (selected) View.VISIBLE else View.GONE
            }
        }

        dialogBinding.rowHomeScreen.setOnClickListener {
            renderTargetSelection(WALLPAPER_TARGET_HOME)
        }
        dialogBinding.rowLockScreen.setOnClickListener {
            renderTargetSelection(WALLPAPER_TARGET_LOCK)
        }
        dialogBinding.rowHomeLockScreen.setOnClickListener {
            renderTargetSelection(WALLPAPER_TARGET_BOTH)
        }
        dialogBinding.btnCancelWallpaper.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnApplyWallpaper.setOnClickListener {
            dialog.dismiss()
            applySelectedPhotoAsWallpaper(selectedTarget)
        }

        renderTargetSelection(WALLPAPER_TARGET_HOME)
    }

    private fun applySelectedPhotoAsWallpaper(target: Int) {
        val mediaUri = selectedMediaUri ?: run {
            Toast.makeText(requireContext(), "Photo not found", Toast.LENGTH_SHORT).show()
            return
        }
        val applyingDialog = showApplyingWallpaperDialog()

        viewLifecycleOwner.lifecycleScope.launch {
            val progressJob = launch {
                for (progress in 0..90) {
                    updateWallpaperProgress(applyingDialog.binding, progress)
                    delay(WALLPAPER_PROGRESS_STEP_MS)
                }
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = requireContext().contentResolver.openInputStream(mediaUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    } ?: error("Unable to open photo")
                    val wallpaperManager =
                        WallpaperManager.getInstance(requireContext().applicationContext)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val flags = when (target) {
                            WALLPAPER_TARGET_LOCK -> WallpaperManager.FLAG_LOCK
                            WALLPAPER_TARGET_BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            else -> WallpaperManager.FLAG_SYSTEM
                        }
                        wallpaperManager.setBitmap(bitmap, null, true, flags)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
            }

            progressJob.cancel()
            updateWallpaperProgress(applyingDialog.binding, 100)
            delay(WALLPAPER_DIALOG_FINISH_DELAY_MS)
            if (!isAdded) return@launch

            applyingDialog.dialog.dismiss()
            Toast.makeText(
                requireContext(),
                if (result.isSuccess) "Wallpaper applied" else "Unable to set wallpaper",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateWallpaperProgress(
        dialogBinding: DialogApplyingWallpaperBinding,
        progress: Int
    ) {
        val normalizedProgress = progress.coerceIn(0, 100)
        dialogBinding.progressWallpaper.progress = normalizedProgress
        dialogBinding.tvWallpaperPercent.text = "$normalizedProgress%"
    }

    private data class WallpaperTargetViews(
        val row: View,
        val icon: android.widget.TextView,
        val check: View,
        val target: Int
    )

    private data class ApplyingWallpaperDialog(
        val dialog: Dialog,
        val binding: DialogApplyingWallpaperBinding
    )

    private fun showApplyingWallpaperDialog(): ApplyingWallpaperDialog {
        val dialogBinding = DialogApplyingWallpaperBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCancelable(false)
            show()
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
        return ApplyingWallpaperDialog(dialog, dialogBinding)
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
        const val WALLPAPER_PROGRESS_STEP_MS = 18L
        const val WALLPAPER_DIALOG_FINISH_DELAY_MS = 350L
        const val WALLPAPER_TARGET_HOME = 1
        const val WALLPAPER_TARGET_LOCK = 2
        const val WALLPAPER_TARGET_BOTH = 3
    }
}
