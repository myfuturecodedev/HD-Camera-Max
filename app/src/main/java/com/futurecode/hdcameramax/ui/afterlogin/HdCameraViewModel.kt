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
    private var recordingTimerJob: Job? = null

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
        val openingSelector = !state.isGridSelectorVisible
        _uiState.value = state.copy(
            isGridSelectorVisible = openingSelector,
            isSettingsPanelVisible = true,
            isTimerStripVisible = false,
            isFocusSelectorVisible = false,
            selectedGridGuide = if (openingSelector && !state.isGridEnabled) "3×3" else state.selectedGridGuide,
            isGridEnabled = if (openingSelector) true else state.isGridEnabled
        )
    }

    fun selectGridGuide(guide: String) {
        _uiState.value = currentState().copy(
            selectedGridGuide = guide,
            isGridEnabled = guide != "None"
        )
    }

    fun hideGridSelector() {
        _uiState.value = currentState().copy(isGridSelectorVisible = false)
    }

    fun toggleFocusSelector() {
        val state = currentState()
        _uiState.value = state.copy(
            isFocusSelectorVisible = !state.isFocusSelectorVisible,
            isSettingsPanelVisible = true,
            isGridSelectorVisible = false,
            isTimerStripVisible = false
        )
    }

    fun selectFocusMode(mode: String) {
        _uiState.value = currentState().copy(
            selectedFocusMode = mode,
            isFaceOverlayEnabled = mode != "Auto Focus"
        )
    }

    fun hideFocusSelector() {
        _uiState.value = currentState().copy(isFocusSelectorVisible = false)
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
        _uiState.value = state.copy(
            isSettingsPanelVisible = !state.isSettingsPanelVisible,
            isTimerStripVisible = false,
            isGridSelectorVisible = false,
            isFocusSelectorVisible = false
        )
    }

    fun hideSettingsPanel() {
        _uiState.value = currentState().copy(isSettingsPanelVisible = false)
    }

    fun toggleTimerStrip() {
        val state = currentState()
        _uiState.value = state.copy(
            isTimerStripVisible = !state.isTimerStripVisible,
            isSettingsPanelVisible = false,
            isGridSelectorVisible = false,
            isFocusSelectorVisible = false
        )
    }

    fun hideTransientTopPanels() {
        _uiState.value = currentState().copy(
            isSettingsPanelVisible = false,
            isTimerStripVisible = false,
            isGridSelectorVisible = false,
            isFocusSelectorVisible = false
        )
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
        _uiState.value = currentState().copy(
            timerSeconds = seconds.coerceAtLeast(0),
            isTimerStripVisible = false
        )
    }

    fun updateZoom(zoomRatio: Float) {
        _uiState.value = currentState().copy(zoomRatio = zoomRatio)
    }

    fun updateExposure(exposureValue: Float) {
        _uiState.value = currentState().copy(exposureValue = exposureValue)
    }

    fun setMode(mode: CameraAppMode) {
        countdownJob?.cancel()
        if (mode != CameraAppMode.VIDEO) {
            stopRecordingTimer()
        }
        _uiState.value = currentState().copy(
            activeMode = mode,
            isSettingsPanelVisible = false,
            isTimerStripVisible = false,
            isGridSelectorVisible = false,
            isFocusSelectorVisible = false,
            isCapturing = false,
            countdownValue = 0,
            recordingElapsedSeconds = if (mode == CameraAppMode.VIDEO) {
                currentState().recordingElapsedSeconds
            } else {
                0
            }
        )
    }

    fun setAspectRatio(label: String) {
        _uiState.value = currentState().copy(
            aspectRatioLabel = label
        )
    }

    fun showAspectSelector() {
        _uiState.value = currentState().copy(
            isSettingsPanelVisible = true,
            isGridSelectorVisible = false,
            isFocusSelectorVisible = false,
            isTimerStripVisible = false
        )
    }

    fun setFilter(filterName: String) {
        _uiState.value = currentState().copy(selectedFilter = filterName)
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

            val latestState = currentState()
            if (latestState.activeMode == CameraAppMode.VIDEO) {
                _videoRecordRequest.value = System.currentTimeMillis()
            } else {
                _captureRequest.value = System.currentTimeMillis()
            }
        }
    }

    fun markVideoRecordingStarted() {
        countdownJob?.cancel()
        startRecordingTimer()
        _uiState.value = currentState().copy(
            isCapturing = false,
            isRecordingVideo = true,
            countdownValue = 0,
            recordingElapsedSeconds = 0
        )
    }

    fun finishVideoRecording() {
        countdownJob?.cancel()
        stopRecordingTimer()
        viewModelScope.launch {
            val latestUri = withContext(Dispatchers.IO) {
                repository.loadLatestMediaUri()
            }
            _uiState.value = currentState().copy(
                isCapturing = false,
                isRecordingVideo = false,
                countdownValue = 0,
                recordingElapsedSeconds = 0,
                latestMediaUri = latestUri
            )
        }
    }

    fun failVideoRecording() {
        countdownJob?.cancel()
        stopRecordingTimer()
        _uiState.value = currentState().copy(
            isCapturing = false,
            isRecordingVideo = false,
            countdownValue = 0,
            recordingElapsedSeconds = 0
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
        stopRecordingTimer()
        _uiState.value = currentState().copy(
            isCapturing = false,
            isRecordingVideo = false,
            countdownValue = 0,
            recordingElapsedSeconds = 0
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

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            while (true) {
                _uiState.value = currentState().copy(
                    isRecordingVideo = true,
                    recordingElapsedSeconds = elapsedSeconds
                )
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    override fun onCleared() {
        countdownJob?.cancel()
        stopRecordingTimer()
        super.onCleared()
    }

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
