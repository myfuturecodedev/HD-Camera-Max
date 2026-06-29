package com.futurecode.hdcameramax.model

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val dateAdded: Long, // Grouping calculations ke liye mandatory hai
    val formattedDate: String
)