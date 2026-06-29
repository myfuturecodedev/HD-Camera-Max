package com.futurecode.hdcameramax.ui.afterlogin

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.futurecode.hdcameramax.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardRepository(private val context: Context) {

    @SuppressLint("Range")
    fun loadRecentPhotos(limit: Int = 4): List<MediaItem> {
        val recentItems = mutableListOf<MediaItem>()
        val collectionUri = MediaStore.Files.getContentUri("external")
        val targetSubFolder = "%DCIM/CameraHDMax%"
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
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return try {
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                while (cursor.moveToNext() && recentItems.size < limit) {
                    val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                    val mediaType =
                        cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))
                    val dateAdded =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))
                    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val baseUri = if (isVideo) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val dateInMillis = dateAdded * 1000
                    recentItems.add(
                        MediaItem(
                            uri = ContentUris.withAppendedId(baseUri, id),
                            isVideo = isVideo,
                            dateAdded = dateInMillis,
                            formattedDate = formatter.format(Date(dateInMillis))
                        )
                    )
                }
            }
            recentItems
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
