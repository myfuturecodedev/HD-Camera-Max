package com.futurecode.hdcameramax.ui.afterlogin

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.futurecode.hdcameramax.model.ResolutionPreset

class HdCameraRepository(private val context: Context) {

    fun getResolutionPresets(): List<ResolutionPreset> {
        return listOf(
            ResolutionPreset(4080, 3072, "4:3", "Ultra HD", isRecommended = true),
            ResolutionPreset(4096, 2304, "16:9", "Ultra HD"),
            ResolutionPreset(4096, 1848, "Full", "Ultra HD"),
            ResolutionPreset(3840, 2160, "16:9", "Ultra HD"),
            ResolutionPreset(3712, 2784, "4:3", "High Quality"),
            ResolutionPreset(3712, 2088, "16:9", "High Quality"),
            ResolutionPreset(3712, 1680, "Full", "High Quality"),
            ResolutionPreset(3264, 2448, "4:3", "High Quality"),
            ResolutionPreset(3264, 1840, "16:9", "High Quality"),
            ResolutionPreset(3264, 1472, "Full", "High Quality"),
            ResolutionPreset(3200, 1440, "Full", "Standard"),
            ResolutionPreset(3168, 1440, "Full", "Standard"),
            ResolutionPreset(3136, 1440, "Full", "Standard"),
            ResolutionPreset(3072, 3072, "1:1", "Standard"),
            ResolutionPreset(2944, 1656, "16:9", "Standard"),
            ResolutionPreset(2560, 1920, "4:3", "Standard"),
            ResolutionPreset(2560, 1440, "16:9", "Standard"),
            ResolutionPreset(1920, 1440, "4:3", "Standard"),
            ResolutionPreset(1920, 1080, "16:9", "Standard"),
            ResolutionPreset(1440, 1440, "1:1", "Standard")
        )
    }

    @SuppressLint("Range")
    fun loadLatestMediaUri(): Uri? {
        val collectionUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val selection =
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "%DCIM/CameraHDMax%"
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        return try {
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                val mediaType =
                    cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))
                val baseUri = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                ContentUris.withAppendedId(baseUri, id)
            }
        } catch (_: SecurityException) {
            null
        }
    }
}
