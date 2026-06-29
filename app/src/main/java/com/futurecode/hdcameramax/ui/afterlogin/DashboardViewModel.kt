package com.futurecode.hdcameramax.ui.afterlogin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(DashboardUiState())
    val uiState: LiveData<DashboardUiState> = _uiState

    fun refreshDashboard(hasCameraPermission: Boolean) {
        _uiState.value = _uiState.value.orEmpty().copy(
            hasCameraPermission = hasCameraPermission,
            isRecentPhotosLoading = true
        )

        viewModelScope.launch {
            val recentPhotos = withContext(Dispatchers.IO) {
                repository.loadRecentPhotos()
            }

            _uiState.value = _uiState.value.orEmpty().copy(
                recentPhotos = recentPhotos,
                isRecentPhotosLoading = false,
                hasCameraPermission = hasCameraPermission
            )
        }
    }

    fun updateCameraPermission(granted: Boolean) {
        _uiState.value = _uiState.value.orEmpty().copy(hasCameraPermission = granted)
    }

    private fun DashboardUiState?.orEmpty(): DashboardUiState = this ?: DashboardUiState()

    class Factory(
        private val repository: DashboardRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
