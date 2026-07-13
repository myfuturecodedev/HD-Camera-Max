package com.futurecode.hdcameramax.ui.afterlogin

import android.net.Uri
import androidx.camera.core.ImageCapture
import com.futurecode.hdcameramax.model.CameraAppMode
import com.futurecode.hdcameramax.model.ResolutionPreset

data class HdCameraUiState(
    val hasCameraPermission: Boolean = false,
    val isCameraReady: Boolean = false,
    val isCapturing: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val recordingElapsedSeconds: Int = 0,
    val isGridEnabled: Boolean = false,
    val selectedGridGuide: String = "None",
    val isGridSelectorVisible: Boolean = false,
    val isHdrEnabled: Boolean = false,
    val isFaceOverlayEnabled: Boolean = false,
    val selectedFocusMode: String = "Macro Focus",
    val isFocusSelectorVisible: Boolean = false,
    val isSettingsPanelVisible: Boolean = false,
    val isTimerStripVisible: Boolean = false,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val timerSeconds: Int = 0,
    val countdownValue: Int = 0,
    val zoomRatio: Float = 1f,
    val exposureValue: Float = 0f,
    val aspectRatioLabel: String = "4:3",
    val selectedFilter: String = "Default",
    val selectedWhiteBalance: String = "Auto",
    val isWhiteBalanceStripVisible: Boolean = false,
    val activeMode: CameraAppMode = CameraAppMode.PHOTO,
    val selectedResolution: ResolutionPreset? = null,
    val resolutionPresets: List<ResolutionPreset> = emptyList(),
    val latestMediaUri: Uri? = null
)
