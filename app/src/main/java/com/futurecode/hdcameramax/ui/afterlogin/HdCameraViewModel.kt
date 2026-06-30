package com.futurecode.hdcameramax.ui.afterlogin

import androidx.camera.core.ImageCapture
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.futurecode.hdcameramax.model.CameraAppMode
import com.futurecode.hdcameramax.model.ResolutionPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HdCameraViewModel(
    private val repository: HdCameraRepository
) : ViewModel() {

    private val initialPresets = repository.getResolutionPresets()

    private val _uiState = MutableLiveData(
        HdCameraUiState(
            resolutionPresets = initialPresets,
            selectedResolution = initialPresets.firstOrNull()
        )
    )
    val uiState: LiveData<HdCameraUiState> = _uiState

    private val _captureRequest = MutableLiveData<Long>()
    val captureRequest: LiveData<Long> = _captureRequest

    private val _videoRecordRequest = MutableLiveData<Long>()
    val videoRecordRequest: LiveData<Long> = _videoRecordRequest

    private var countdownJob: Job? = null

    fun refreshGalleryPreview() {
        viewModelScope.launch {
            val latestUri = withContext(Dispatchers.IO) {
                repository.loadLatestMediaUri()
            }
            _uiState.value = currentState().copy(latestMediaUri = latestUri)
        }
    }

    fun updatePermission(granted: Boolean) {
        _uiState.value = currentState().copy(hasCameraPermission = granted)
    }

    fun markCameraReady() {
        _uiState.value = currentState().copy(isCameraReady = true)
    }

    fun markCameraUnavailable() {
        _uiState.value = currentState().copy(isCameraReady = false)
    }

    fun toggleGrid() {
        val state = currentState()
        _uiState.value = state.copy(isGridEnabled = !state.isGridEnabled)
    }

    fun toggleHdr() {
        val state = currentState()
        _uiState.value = state.copy(isHdrEnabled = !state.isHdrEnabled)
    }

    fun toggleFaceOverlay() {
        val state = currentState()
        _uiState.value = state.copy(isFaceOverlayEnabled = !state.isFaceOverlayEnabled)
    }

    fun toggleSettingsPanel() {
        val state = currentState()
        _uiState.value = state.copy(isSettingsPanelVisible = !state.isSettingsPanelVisible)
    }

    fun hideSettingsPanel() {
        _uiState.value = currentState().copy(isSettingsPanelVisible = false)
    }

    fun setFlashMode(mode: Int) {
        _uiState.value = currentState().copy(flashMode = mode)
    }

    fun cycleTimer() {
        val nextTimer = when (currentState().timerSeconds) {
            0 -> 3
            3 -> 5
            5 -> 10
            else -> 0
        }
        _uiState.value = currentState().copy(timerSeconds = nextTimer)
    }

    fun setTimer(seconds: Int) {
        _uiState.value = currentState().copy(timerSeconds = seconds.coerceAtLeast(0))
    }

    fun updateZoom(zoomRatio: Float) {
        _uiState.value = currentState().copy(zoomRatio = zoomRatio)
    }

    fun updateExposure(exposureValue: Float) {
        _uiState.value = currentState().copy(exposureValue = exposureValue)
    }

    fun setMode(mode: CameraAppMode) {
        _uiState.value = currentState().copy(
            activeMode = mode,
            isSettingsPanelVisible = false
        )
    }

    fun setAspectRatio(label: String) {
        _uiState.value = currentState().copy(aspectRatioLabel = label)
    }

    fun selectResolution(preset: ResolutionPreset) {
        _uiState.value = currentState().copy(
            selectedResolution = preset,
            aspectRatioLabel = preset.ratioLabel
        )
    }

    fun requestCapture() {
        val state = currentState()
        if (state.activeMode == CameraAppMode.VIDEO && state.isRecordingVideo) {
            _videoRecordRequest.value = System.currentTimeMillis()
            return
        }

        if (state.isCapturing || !state.isCameraReady) return

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            if (state.timerSeconds > 0) {
                for (remaining in state.timerSeconds downTo 1) {
                    _uiState.value = currentState().copy(
                        isCapturing = true,
                        countdownValue = remaining
                    )
                    delay(1000)
                }
            }

            _uiState.value = currentState().copy(
                isCapturing = true,
                countdownValue = 0
            )

            if (state.activeMode == CameraAppMode.VIDEO) {
                _videoRecordRequest.value = System.currentTimeMillis()
            } else {
                _captureRequest.value = System.currentTimeMillis()
            }
        }
    }

    fun markVideoRecordingStarted() {
        countdownJob?.cancel()
        _uiState.value = currentState().copy(
            isCapturing = false,
            isRecordingVideo = true,
            countdownValue = 0
        )
    }

    fun finishVideoRecording() {
        countdownJob?.cancel()
        viewModelScope.launch {
            val latestUri = withContext(Dispatchers.IO) {
                repository.loadLatestMediaUri()
            }
            _uiState.value = currentState().copy(
                isCapturing = false,
                isRecordingVideo = false,
                countdownValue = 0,
                latestMediaUri = latestUri
            )
        }
    }

    fun failVideoRecording() {
        countdownJob?.cancel()
        _uiState.value = currentState().copy(
            isCapturing = false,
            isRecordingVideo = false,
            countdownValue = 0
        )
    }

    fun finishCapture() {
        countdownJob?.cancel()
        viewModelScope.launch {
            val latestUri = withContext(Dispatchers.IO) {
                repository.loadLatestMediaUri()
            }
            _uiState.value = currentState().copy(
                isCapturing = false,
                countdownValue = 0,
                latestMediaUri = latestUri
            )
        }
    }

    fun cancelCaptureFlow() {
        countdownJob?.cancel()
        _uiState.value = currentState().copy(
            isCapturing = false,
            countdownValue = 0
        )
    }

    fun nextFlashMode(): Int {
        return when (currentState().flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
    }

    private fun currentState(): HdCameraUiState = _uiState.value ?: HdCameraUiState()

    class Factory(
        private val repository: HdCameraRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HdCameraViewModel::class.java)) {
                return HdCameraViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
