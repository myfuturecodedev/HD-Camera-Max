package com.futurecode.hdcameramax.ui.afterlogin

import com.futurecode.hdcameramax.model.MediaItem

data class DashboardUiState(
    val recentPhotos: List<MediaItem> = emptyList(),
    val isRecentPhotosLoading: Boolean = false,
    val hasCameraPermission: Boolean = false
)
