package com.futurecode.hdcameramax.model

enum class CameraAppMode { VIDEO, PHOTO, PORTRAIT }

data class ResolutionPreset(
    val width: Int,
    val height: Int,
    val ratioLabel: String,
    val qualityGroup: String,
    val isRecommended: Boolean = false
) {
    val displayString: String get() = "${width} × ${height}"
    val aspectRatioType: Int get() = if (ratioLabel == "16:9") 1 else 0
}