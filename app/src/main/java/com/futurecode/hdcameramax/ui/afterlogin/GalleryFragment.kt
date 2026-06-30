package com.futurecode.hdcameramax.ui.afterlogin

import android.annotation.SuppressLint
import android.content.ContentUris
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.adapter.GalleryAdapter
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentGalleryBinding
import com.futurecode.hdcameramax.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GalleryFragment : BaseFragment<FragmentGalleryBinding>(FragmentGalleryBinding::inflate) {

    private lateinit var todayAdapter: GalleryAdapter
    private lateinit var yesterdayAdapter: GalleryAdapter
    private lateinit var lastWeekAdapter: GalleryAdapter

    private var allAppMediaList = mutableListOf<MediaItem>()
    private var isVideoTabSelected = false // Status tracker matching your prompt criteria

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerViews()
        setupTabClickListeners()

        // Background indexing trigger to scan app directory safely
        loadAppSpecificMedia()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initRecyclerViews() {
        todayAdapter = GalleryAdapter(emptyList()) { handleMediaClick(it) }
        yesterdayAdapter = GalleryAdapter(emptyList()) { handleMediaClick(it) }
        lastWeekAdapter = GalleryAdapter(emptyList()) { handleMediaClick(it) }

        binding.rvToday.adapter = todayAdapter
        binding.rvYesterday.adapter = yesterdayAdapter
        binding.rvLastWeek.adapter = lastWeekAdapter
    }

    private fun setupTabClickListeners() {
        // PICTURE TAB SELECTION ACTIONS
        binding.btnPicture.setOnClickListener {
            if (isVideoTabSelected) {
                isVideoTabSelected = false
                updateTabUIStates(isPictureSelected = true) // ✅ Active picture state
                filterAndRenderUiLists()
            }
        }

        // VIDEO TAB SELECTION ACTIONS
        binding.btnVideo.setOnClickListener {
            if (!isVideoTabSelected) {
                isVideoTabSelected = true
                updateTabUIStates(isPictureSelected = false) // 👑 FIXED: Changed from 'true' to 'false' to render video state correctly
                filterAndRenderUiLists()
            }
        }
    }

    private fun updateTabUIStates(isPictureSelected: Boolean) {
        if (isPictureSelected) {
            binding.btnPicture.setBackgroundResource(R.drawable.bg_active_tab)
            binding.btnPicture.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnPicture.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.white)

            binding.btnVideo.setBackgroundResource(android.R.color.transparent)
            binding.btnVideo.setTextColor(Color.parseColor("#7C7C8A"))
            binding.btnVideo.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.text_gray_dim)
        } else {
            binding.btnPicture.setBackgroundResource(android.R.color.transparent)
            binding.btnPicture.setTextColor(Color.parseColor("#7C7C8A"))
            binding.btnPicture.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.text_gray_dim)

            binding.btnVideo.setBackgroundResource(R.drawable.bg_active_tab)
            binding.btnVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnVideo.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.white)
        }
    }

    @SuppressLint("Range")
    private fun loadAppSpecificMedia() {
        // Coroutines are triggered to run I/O scanning asynchronously away from main thread loops
        viewLifecycleOwner.lifecycleScope.launch {
            val fetchedMedia = withContext(Dispatchers.IO) {
                val appMedia = mutableListOf<MediaItem>()
                val targetSubFolder = "%DCIM/CameraHDMax%" // Matches your exact app save storage bucket label

                val collectionUri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.RELATIVE_PATH
                )

                // Selection query isolates only images and videos from our structural folder pathway
                val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                    targetSubFolder
                )

                val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

                context?.contentResolver?.query(collectionUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                        val mediaType = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))
                        val dateAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))

                        val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        val baseContentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        val itemUri = ContentUris.withAppendedId(baseContentUri, id)

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formattedDate = sdf.format(Date(dateAdded * 1000))

                        appMedia.add(MediaItem(itemUri, isVideo, dateAdded * 1000, formattedDate))
                    }
                }
                return@withContext appMedia
            }

            allAppMediaList.clear()
            allAppMediaList.addAll(fetchedMedia)

            // Render default list representation initial cycle setup
            filterAndRenderUiLists()
        }
    }

    private fun filterAndRenderUiLists() {
        // Tab filtering layer loop metrics
        val targetedFilteredList = allAppMediaList.filter { it.isVideo == isVideoTabSelected }

        val todayCalendar = Calendar.getInstance()
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(todayCalendar.time)
        val yesterdayStr = sdf.format(yesterdayCalendar.time)

        // Date segment partitioning sorting criteria
        val todayItems = targetedFilteredList.filter { it.formattedDate == todayStr }
        val yesterdayItems = targetedFilteredList.filter { it.formattedDate == yesterdayStr }
        val lastWeekItems = targetedFilteredList.filter { it.formattedDate != todayStr && it.formattedDate != yesterdayStr }

        // Pushing separated modules down to respective multi-grid subviews loaders adapters updates
        todayAdapter.updateData(todayItems)
        yesterdayAdapter.updateData(yesterdayItems)
        lastWeekAdapter.updateData(lastWeekItems)
    }

    private fun handleMediaClick(item: MediaItem) {
        val args = Bundle().apply {
            putString(PhotoAndVideoViewFragment.ARG_MEDIA_URI, item.uri.toString())
            putBoolean(PhotoAndVideoViewFragment.ARG_IS_VIDEO, item.isVideo)
            putLong(PhotoAndVideoViewFragment.ARG_DATE_ADDED, item.dateAdded)
        }
        findNavController().navigate(
            R.id.action_galleryFragment_to_photoAndVideoViewFragment,
            args
        )
    }
}
