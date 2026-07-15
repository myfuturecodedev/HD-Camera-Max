package com.futurecode.hdcameramax.ui.afterlogin

import android.annotation.SuppressLint
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentFavouriteBinding
import com.futurecode.hdcameramax.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FavouriteFragment : BaseFragment<FragmentFavouriteBinding>(FragmentFavouriteBinding::inflate) {

    private lateinit var todayAdapter: FavouriteAdapter
    private lateinit var yesterdayAdapter: FavouriteAdapter
    private lateinit var lastWeekAdapter: FavouriteAdapter
    private lateinit var favouriteRepository: FavouriteRepository

    private var nativeAdsHelper: NativeAdsHelper? = null
    private var fullScreenAdsHelper: FullScreenAdsHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favouriteRepository = FavouriteRepository(requireContext())
        setupRecyclerViews()
        setupClickListeners()
        loadFavouriteMedia()

        nativeAdsHelper= NativeAdsHelper(requireActivity())
        fullScreenAdsHelper= FullScreenAdsHelper(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        if (::favouriteRepository.isInitialized) {
            loadFavouriteMedia()
        }
    }

    private fun setupRecyclerViews() {
        todayAdapter = createAdapter()
        yesterdayAdapter = createAdapter()
        lastWeekAdapter = createAdapter()

        binding.rvToday.adapter = todayAdapter
        binding.rvYesterday.adapter = yesterdayAdapter
        binding.rvLastWeek.adapter = lastWeekAdapter
    }

    private fun createAdapter(): FavouriteAdapter {
        return FavouriteAdapter(
            items = emptyList(),
            onItemClick = { openMediaPreview(it) },
            onRemoveFavourite = { removeFavourite(it) }
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFav.setOnClickListener {
            loadFavouriteMedia()
        }
    }

    @SuppressLint("Range")
    private fun loadFavouriteMedia() {
        viewLifecycleOwner.lifecycleScope.launch {
            val favourites = withContext(Dispatchers.IO) {
                val favouriteUriStrings = favouriteRepository.favouriteUris()
                if (favouriteUriStrings.isEmpty()) {
                    return@withContext emptyList<MediaItem>()
                }

                val mediaItems = mutableListOf<MediaItem>()
                val targetSubFolder = "%DCIM/CameraHDMax%"
                val collectionUri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.RELATIVE_PATH
                )
                val selection =
                    "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                    targetSubFolder
                )
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

                context?.contentResolver?.query(collectionUri, projection, selection, selectionArgs, sortOrder)
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                            val mediaType = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))
                            val dateAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))
                            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                            val baseContentUri = if (isVideo) {
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            } else {
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            }
                            val itemUri = ContentUris.withAppendedId(baseContentUri, id)
                            if (favouriteUriStrings.contains(itemUri.toString())) {
                                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(dateAdded * 1000))
                                mediaItems.add(MediaItem(itemUri, isVideo, dateAdded * 1000, formattedDate))
                            }
                        }
                    }

                mediaItems
            }

            renderFavouriteSections(favourites)
        }
    }

    private fun renderFavouriteSections(favourites: List<MediaItem>) {
        val todayCalendar = Calendar.getInstance()
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(todayCalendar.time)
        val yesterdayStr = sdf.format(yesterdayCalendar.time)

        val todayItems = favourites.filter { it.formattedDate == todayStr }
        val yesterdayItems = favourites.filter { it.formattedDate == yesterdayStr }
        val lastWeekItems = favourites.filter { it.formattedDate != todayStr && it.formattedDate != yesterdayStr }

        todayAdapter.updateData(todayItems)
        yesterdayAdapter.updateData(yesterdayItems)
        lastWeekAdapter.updateData(lastWeekItems)

        renderSection(
            title = binding.tvTodayTitle,
            meta = binding.tvTodayMeta,
            list = binding.rvToday,
            items = todayItems,
            dateLabel = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(todayCalendar.time)
        )
        renderSection(
            title = binding.tvYesterdayTitle,
            meta = binding.tvYesterdayMeta,
            list = binding.rvYesterday,
            items = yesterdayItems,
            dateLabel = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(yesterdayCalendar.time)
        )
        renderSection(
            title = binding.tvLastWeekTitle,
            meta = binding.tvLastWeekMeta,
            list = binding.rvLastWeek,
            items = lastWeekItems,
            dateLabel = "Older"
        )

        binding.tvEmptyFavourites.visibility = if (favourites.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderSection(
        title: View,
        meta: android.widget.TextView,
        list: View,
        items: List<MediaItem>,
        dateLabel: String
    ) {
        val visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        title.visibility = visibility
        meta.visibility = visibility
        list.visibility = visibility
        meta.text = "$dateLabel · ${items.size} items"
    }

    private fun removeFavourite(item: MediaItem) {
        favouriteRepository.removeFavourite(item.uri)
        Toast.makeText(requireContext(), "Removed from favourites", Toast.LENGTH_SHORT).show()
        loadFavouriteMedia()
    }

    private fun openMediaPreview(item: MediaItem) {
        val args = Bundle().apply {
            putString(PhotoAndVideoViewFragment.ARG_MEDIA_URI, item.uri.toString())
            putBoolean(PhotoAndVideoViewFragment.ARG_IS_VIDEO, item.isVideo)
            putLong(PhotoAndVideoViewFragment.ARG_DATE_ADDED, item.dateAdded)
        }
        findNavController().navigate(
            R.id.action_favouriteFragment_to_photoAndVideoViewFragment,
            args
        )
    }
}
