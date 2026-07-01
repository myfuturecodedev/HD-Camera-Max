package com.futurecode.hdcameramax.ui.afterlogin

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.adapter.ResolutionPresetAdapter
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.DialogResolutionSelectorBinding
import com.futurecode.hdcameramax.databinding.FragmentHdCameraBinding
import com.futurecode.hdcameramax.model.CameraAppMode
import com.futurecode.hdcameramax.model.ResolutionPreset
import com.futurecode.hdcameramax.utils.CameraEngineKit
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import java.util.Locale

/*
 * Previous implementation note:
 * The older direct-control version handled CameraX, flash, zoom, focus, and capture inside
 * fragment click listeners. It has been replaced in-place with MVVM state management below,
 * while keeping this same Fragment class, XML binding, CameraEngineKit, and navigation entry.
 */
class HdCameraFragment : BaseFragment<FragmentHdCameraBinding>(FragmentHdCameraBinding::inflate) {

    private lateinit var cameraKit: CameraEngineKit
    private lateinit var viewModel: HdCameraViewModel
    private var activeExposureIndex = 0
    private var startVideoAfterCameraReady = false

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.updatePermission(granted)
            if (granted) {
                startCameraPreview()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    private val videoPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission()
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission()
            viewModel.updatePermission(cameraGranted)

            if (!cameraGranted) {
                viewModel.failVideoRecording()
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            if (!audioGranted) {
                viewModel.failVideoRecording()
                Toast.makeText(requireContext(), "Audio permission is required for video", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            if (viewModel.uiState.value?.isCameraReady == true) {
                handleVideoRecordingRequest()
            } else {
                startVideoAfterCameraReady = true
                startCameraPreview()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            HdCameraViewModel.Factory(
                HdCameraRepository(requireContext().applicationContext)
            )
        )[HdCameraViewModel::class.java]

        cameraKit = CameraEngineKit(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            viewFinder = binding.cameraViewFinder
        )

        setupCoreClickListeners()
        observeCameraState()
        checkCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshGalleryPreview()
        }
    }

    override fun onDestroyView() {
        if (::cameraKit.isInitialized) {
            viewModel.cancelCaptureFlow()
            cameraKit.release()
        }
        super.onDestroyView()
    }

    private fun checkCameraPermission() {
        val granted = hasCameraPermission()
        viewModel.updatePermission(granted)
        if (granted) {
            startCameraPreview()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraPreview() {
        cameraKit.initializePipeline {
            viewModel.markCameraReady()
            viewModel.refreshGalleryPreview()
            if (startVideoAfterCameraReady) {
                startVideoAfterCameraReady = false
                handleVideoRecordingRequest()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCoreClickListeners() {
        binding.btnHeaderBackAction.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSettings.setOnClickListener {
            viewModel.toggleSettingsPanel()
        }

        binding.btnCameraFlip.setOnClickListener {
            cameraKit.switchLensFacing()
            viewModel.hideSettingsPanel()
        }

        binding.btnFlash.setOnClickListener {
            val state = cameraKit.setFlashMode(viewModel.nextFlashMode())
            viewModel.setFlashMode(state)
        }

//        binding.btnHdrToggle.setOnClickListener {
//            toggleHdr()
//        }

        binding.btnTimerToggle.setOnClickListener {
            showTimerMenu()
        }

//        binding.btnGridToggle.setOnClickListener {
//            viewModel.toggleGrid()
//        }

        binding.btnRatioConfig.setOnClickListener {
            showAspectRatioMenu()
        }

        binding.btnPanelGrid.setOnClickListener {
            viewModel.toggleGrid()
        }

        binding.btnPanelHdr.setOnClickListener {
            toggleHdr()
        }

        binding.btnPanelFace.setOnClickListener {
            viewModel.toggleFaceOverlay()
        }

        binding.btnPanelTimer.setOnClickListener {
            showTimerMenu()
        }

        binding.btnPanelResolution.setOnClickListener {
            showResolutionSelector()
            viewModel.hideSettingsPanel()
        }

        binding.btnPanelAspect.setOnClickListener {
            showAspectRatioMenu()
        }

        binding.filterDefaultOption.setOnClickListener {
            viewModel.setFilter(FILTER_DEFAULT)
        }

        binding.filterFoggyOption.setOnClickListener {
            viewModel.setFilter(FILTER_FOGGY)
        }

        binding.filterDaylightOption.setOnClickListener {
            viewModel.setFilter(FILTER_DAYLIGHT)
        }

        binding.filterSpikeOption.setOnClickListener {
            viewModel.setFilter(FILTER_SPIKE)
        }

        binding.filterGloamOption.setOnClickListener {
            viewModel.setFilter(FILTER_GLOAM)
        }

        binding.btnOpenSettingsScreen.setOnClickListener {
            navigateSafely(R.id.action_hdCameraFragment_to_settingsFragment)
        }

        binding.tvZoom1x.setOnClickListener {
            updateZoomLevel(1.0f, it)
        }

        binding.tvZoom10x.setOnClickListener {
            updateZoomLevel(2.0f, it)
        }

        binding.tvZoom100x.setOnClickListener {
            updateZoomLevel(5.0f, it)
        }

        binding.tvRecordingZoom1x.setOnClickListener {
            updateZoomLevel(1.0f, it)
        }

        binding.tvRecordingZoom10x.setOnClickListener {
            updateZoomLevel(2.0f, it)
        }

        binding.tvRecordingZoom100x.setOnClickListener {
            updateZoomLevel(5.0f, it)
        }

        binding.sbDynamicZoomSeeker.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val actualScale = 1.0f + (progress / 100f) * 9f
                        cameraKit.applyZoomRatio(actualScale)
                        viewModel.updateZoom(actualScale)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) = Unit

                override fun onStopTrackingTouch(sb: SeekBar?) = Unit
            }
        )

        binding.cameraViewFinder.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                cameraKit.triggerManualFocus(event.x, event.y)
                renderInteractiveFocusSquare(event.x, event.y)
                view.performClick()
                true
            } else {
                false
            }
        }

        binding.btnExposurePlus.setOnClickListener {
            if (activeExposureIndex < 4) {
                activeExposureIndex++
                applyExposureChange()
            }
        }

        binding.btnExposureMinus.setOnClickListener {
            if (activeExposureIndex > -4) {
                activeExposureIndex--
                applyExposureChange()
            }
        }

        binding.tvTabVideoMode.setOnClickListener {
            switchUIMode(CameraAppMode.VIDEO)
        }

        binding.tvTabPhotoMode.setOnClickListener {
            switchUIMode(CameraAppMode.PHOTO)
        }

        binding.tvTabPortraitMode.setOnClickListener {
            switchUIMode(CameraAppMode.PORTRAIT)
        }

        binding.ivActionShutter.setOnClickListener {
            viewModel.requestCapture()
        }

        binding.ivGalleryThumbShortcut.setOnClickListener {
            navigateSafely(R.id.action_hdCameraFragment_to_galleryFragment)
        }

        binding.cameraControlsScrim.setOnClickListener {
            viewModel.hideSettingsPanel()
        }
    }

    private fun observeCameraState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderUiState(state)
        }

        viewModel.captureRequest.observe(viewLifecycleOwner) {
            capturePhoto()
        }

        viewModel.videoRecordRequest.observe(viewLifecycleOwner) {
            handleVideoRecordingRequest()
        }
    }

    private fun toggleHdr() {
        val enableHdr = viewModel.uiState.value?.isHdrEnabled != true
        cameraKit.setHdrQualityEnabled(enableHdr)
        viewModel.toggleHdr()
    }

    private fun updateZoomLevel(scale: Float, selectedView: View) {
        cameraKit.applyZoomRatio(scale)
        viewModel.updateZoom(scale)
        selectedView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)

        binding.sbDynamicZoomSeeker.progress = (((scale - 1f) / 9f) * 100).toInt()
    }

    private fun applyExposureChange() {
        val exposureValue = cameraKit.setExposureLevel(activeExposureIndex)
        viewModel.updateExposure(exposureValue)
    }

    private fun switchUIMode(mode: CameraAppMode) {
        if (viewModel.uiState.value?.isRecordingVideo == true && mode != CameraAppMode.VIDEO) {
            cameraKit.stopVideoRecording()
            Toast.makeText(requireContext(), "Stopping recording", Toast.LENGTH_SHORT).show()
            return
        }
        cameraKit.switchAppMode(mode)
        viewModel.setMode(mode)
    }

    private fun renderInteractiveFocusSquare(x: Float, y: Float) {
        binding.viewFocusFrameIndicator.x = x - binding.viewFocusFrameIndicator.width / 2f
        binding.viewFocusFrameIndicator.y = y - binding.viewFocusFrameIndicator.height / 2f

        binding.viewFocusFrameIndicator.alpha = 1f
        binding.viewFocusFrameIndicator.visibility = View.VISIBLE
        binding.viewFocusFrameIndicator.animate()
            .alpha(0f)
            .setStartDelay(1200)
            .setDuration(400)
            .start()
    }

    private fun renderUiState(state: HdCameraUiState) {
        val isRecordingVideo = state.activeMode == CameraAppMode.VIDEO && state.isRecordingVideo
        val showSettingsPanel = state.isSettingsPanelVisible && !isRecordingVideo

        binding.viewGridLines.visibility = if (state.isGridEnabled) View.VISIBLE else View.GONE
        binding.viewFaceDetectionOverlay.visibility =
            if (state.isFaceOverlayEnabled) View.VISIBLE else View.GONE
        binding.cameraControlsScrim.visibility =
            if (showSettingsPanel) View.VISIBLE else View.GONE
        binding.cameraSettingsPanel.visibility =
            if (showSettingsPanel) View.VISIBLE else View.GONE
        binding.tvCountdownOverlay.visibility =
            if (state.countdownValue > 0) View.VISIBLE else View.GONE
        binding.topBarHeaderControls.visibility = if (isRecordingVideo) View.GONE else View.VISIBLE
        binding.llStatusBadges.visibility = if (isRecordingVideo) View.GONE else View.VISIBLE
        binding.llInteractiveControlDeck.visibility = if (isRecordingVideo) View.GONE else View.VISIBLE
        binding.llFilterSelectorStrip.visibility = if (isRecordingVideo) View.GONE else View.VISIBLE
        binding.llRecordingZoomChips.visibility = if (isRecordingVideo) View.VISIBLE else View.GONE
        binding.tvRecordingTimerPill.visibility = if (isRecordingVideo) View.VISIBLE else View.GONE

        val shutterEnabled = state.isCameraReady && (!state.isCapturing || state.isRecordingVideo)
        binding.tvCountdownOverlay.text = state.countdownValue.toString()
        binding.ivActionShutter.isEnabled = shutterEnabled
        binding.ivActionShutter.alpha = if (shutterEnabled) 1f else 0.45f
        binding.ivActionShutter.setImageResource(
            when {
                state.activeMode == CameraAppMode.VIDEO && state.isRecordingVideo ->
                    R.drawable.ic_camera_recording_stop_trigger
                state.activeMode == CameraAppMode.VIDEO ->
                    R.drawable.ic_camera_video_trigger
                else -> R.drawable.ic_camera_shutter_trigger
            }
        )
        binding.ivActionShutter.contentDescription = when {
            state.activeMode == CameraAppMode.VIDEO && state.isRecordingVideo ->
                getString(R.string.stop_recording)
            state.activeMode == CameraAppMode.VIDEO ->
                getString(R.string.start_recording)
            else -> getString(R.string.capture)
        }
        binding.tvZoomMaxCapIndicator.text = String.format(Locale.US, "%.1fx", state.zoomRatio)
        binding.tvExposureVal.text = String.format(Locale.US, "%.1f", state.exposureValue)
        binding.tvRecordingTimerPill.text = formatRecordingPillDuration(state.recordingElapsedSeconds)
        binding.tvTimerBadge.text = when {
            state.isRecordingVideo -> "${getString(R.string.recording_short)} ${formatRecordingDuration(state.recordingElapsedSeconds)}"
            state.timerSeconds == 0 -> getString(R.string.off)
            else -> "${state.timerSeconds}s"
        }
        binding.tvTimerBadge.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (state.isRecordingVideo) R.color.permission_green else R.color.white
            )
        )
        binding.tvAspectBadge.text = state.aspectRatioLabel
        binding.tvResolutionValue.text = state.selectedResolution?.displayString ?: "Auto"
        binding.tvHdrBadge.text = if (state.isHdrEnabled) "ON" else "OFF"

        renderFlashIcon(state.flashMode)
        renderToggleState(binding.btnPanelHdr, state.isHdrEnabled)
        renderToggleState(binding.btnPanelGrid, state.isGridEnabled)
        renderToggleState(binding.btnPanelFace, state.isFaceOverlayEnabled)
        renderSettingsPanelLabels(state)
        renderStatusBadgeState(state)
        renderZoomChips(state.zoomRatio)
        renderFilterState(state.selectedFilter)
        renderModeTabs(state.activeMode)
        renderGalleryPreview(state.latestMediaUri)
    }

    private fun renderFlashIcon(flashMode: Int) {
        val icon = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_plain
            ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_live_effects_filter
            else -> R.drawable.ic_flash_off
        }
        binding.btnFlash.setImageResource(icon)
    }

    private fun renderToggleState(view: View, enabled: Boolean) {
        view.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (enabled) R.color.permission_green else R.color.bg_card_dark
        )
    }

    private fun renderSettingsPanelLabels(state: HdCameraUiState) {
        binding.btnPanelGrid.text = getString(
            if (state.isGridEnabled) R.string.grid_on else R.string.grid_off
        )
        binding.btnPanelHdr.text = getString(
            if (state.isHdrEnabled) R.string.hdr_on else R.string.hdr_off
        )
        binding.btnPanelFace.text = getString(
            if (state.isFaceOverlayEnabled) R.string.face_on else R.string.face_off
        )
        binding.btnPanelTimer.text = if (state.timerSeconds == 0) {
            getString(R.string.timer_off)
        } else {
            getString(R.string.timer_seconds_short, state.timerSeconds)
        }
        binding.btnPanelResolution.text =
            state.selectedResolution?.displayString ?: getString(R.string.resolution)
        binding.btnPanelAspect.text = state.aspectRatioLabel
    }

    private fun renderStatusBadgeState(state: HdCameraUiState) {
        binding.tvHdrBadge.setBackgroundResource(
            if (state.isHdrEnabled) R.drawable.bg_camera_chip_active else R.drawable.bg_camera_chip_dark
        )
        binding.tvTimerBadge.setBackgroundResource(
            if (state.timerSeconds > 0 || state.isRecordingVideo) {
                R.drawable.bg_camera_chip_active
            } else {
                R.drawable.bg_camera_chip_dark
            }
        )
        binding.tvAspectBadge.setBackgroundResource(R.drawable.bg_camera_chip_active)
    }

    private fun renderZoomChips(zoomRatio: Float) {
        renderZoomChipSet(zoomRatio, binding.tvZoom1x, binding.tvZoom10x, binding.tvZoom100x)
        renderZoomChipSet(
            zoomRatio,
            binding.tvRecordingZoom1x,
            binding.tvRecordingZoom10x,
            binding.tvRecordingZoom100x
        )
    }

    private fun renderZoomChipSet(
        zoomRatio: Float,
        oneXChip: TextView,
        twoXChip: TextView,
        fiveXChip: TextView
    ) {
        val selectedView = when {
            zoomRatio >= 4.25f -> fiveXChip
            zoomRatio >= 1.5f -> twoXChip
            else -> oneXChip
        }

        val chips = listOf(oneXChip, twoXChip, fiveXChip)
        chips.forEach { chip ->
            val selected = chip == selectedView
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_zoom_btn_selected else R.drawable.bg_zoom_btn_unselected
            )
            chip.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            chip.alpha = if (selected) 1f else 0.78f
        }
    }

    private fun renderFilterState(selectedFilter: String) {
        val filterChips = listOf(
            FilterOption(binding.filterDefaultOption, binding.cardFilterDefault, binding.tvFilterDefault, FILTER_DEFAULT),
            FilterOption(binding.filterFoggyOption, binding.cardFilterFoggy, binding.tvFilterFoggy, FILTER_FOGGY),
            FilterOption(binding.filterDaylightOption, binding.cardFilterDaylight, binding.tvFilterDaylight, FILTER_DAYLIGHT),
            FilterOption(binding.filterSpikeOption, binding.cardFilterSpike, binding.tvFilterSpike, FILTER_SPIKE),
            FilterOption(binding.filterGloamOption, binding.cardFilterGloam, binding.tvFilterGloam, FILTER_GLOAM)
        )

        filterChips.forEach { option ->
            val selected = option.filterName == selectedFilter
            option.card.strokeColor = ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.permission_green else android.R.color.transparent
            )
            option.card.strokeWidth =
                resources.getDimensionPixelSize(if (selected) com.intuit.sdp.R.dimen._2sdp else com.intuit.sdp.R.dimen._1sdp)
            option.label.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.permission_green else R.color.white
                )
            )
            option.label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            option.root.alpha = if (selected) 1f else 0.84f
        }

        val overlayColor = when (selectedFilter) {
            FILTER_FOGGY -> Color.parseColor("#66DDE7F0")
            FILTER_DAYLIGHT -> Color.parseColor("#36FFD36A")
            FILTER_SPIKE -> Color.parseColor("#44FF2B7A")
            FILTER_GLOAM -> Color.parseColor("#66301954")
            else -> Color.TRANSPARENT
        }
        val overlayAlpha = when (selectedFilter) {
            FILTER_FOGGY -> 0.34f
            FILTER_DAYLIGHT -> 0.24f
            FILTER_SPIKE -> 0.30f
            FILTER_GLOAM -> 0.38f
            else -> 0f
        }

        binding.viewFilterOverlay.setBackgroundColor(overlayColor)
        binding.viewFilterOverlay.animate()
            .alpha(overlayAlpha)
            .setDuration(160)
            .start()
    }

    private fun renderModeTabs(mode: CameraAppMode) {
        val activeBackground = R.drawable.bg_camera_mode_active
        val inactiveBackground = R.drawable.bg_camera_mode_inactive

        binding.tvTabVideoMode.setTypeface(
            null,
            if (mode == CameraAppMode.VIDEO) Typeface.BOLD else Typeface.NORMAL
        )
        binding.tvTabPhotoMode.setTypeface(
            null,
            if (mode == CameraAppMode.PHOTO) Typeface.BOLD else Typeface.NORMAL
        )
        binding.tvTabPortraitMode.setTypeface(
            null,
            if (mode == CameraAppMode.PORTRAIT) Typeface.BOLD else Typeface.NORMAL
        )

        binding.tvTabVideoMode.setBackgroundResource(if (mode == CameraAppMode.VIDEO) activeBackground else inactiveBackground)
        binding.tvTabPhotoMode.setBackgroundResource(if (mode == CameraAppMode.PHOTO) activeBackground else inactiveBackground)
        binding.tvTabPortraitMode.setBackgroundResource(if (mode == CameraAppMode.PORTRAIT) activeBackground else inactiveBackground)

        binding.tvTabVideoMode.setTextColor(
            ContextCompat.getColor(requireContext(), if (mode == CameraAppMode.VIDEO) R.color.white else R.color.text_gray_dim)
        )
        binding.tvTabPhotoMode.setTextColor(
            ContextCompat.getColor(requireContext(), if (mode == CameraAppMode.PHOTO) R.color.white else R.color.text_gray_dim)
        )
        binding.tvTabPortraitMode.setTextColor(
            ContextCompat.getColor(requireContext(), if (mode == CameraAppMode.PORTRAIT) R.color.white else R.color.text_gray_dim)
        )
    }

    private fun renderGalleryPreview(uri: android.net.Uri?) {
        if (uri == null) {
            binding.ivGalleryThumbShortcut.setImageResource(R.drawable.ic_gallery_thumb_frame)
        } else {
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_gallery_thumb_frame)
                .centerCrop()
                .into(binding.ivGalleryThumbShortcut)
        }
    }

    private fun handleVideoRecordingRequest() {
        val state = viewModel.uiState.value ?: return

        if (state.isRecordingVideo) {
            cameraKit.stopVideoRecording()
            return
        }

        if (state.activeMode != CameraAppMode.VIDEO) {
            viewModel.failVideoRecording()
            return
        }

        if (!hasCameraPermission() || !hasAudioPermission()) {
            videoPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
            return
        }

        viewModel.hideSettingsPanel()
        cameraKit.startVideoRecording(
            onStarted = {
                viewModel.markVideoRecordingStarted()
                showToast("Recording started")
            },
            onFinalized = {
                viewModel.finishVideoRecording()
                showToast("Video saved")
            },
            onFailure = { error ->
                viewModel.failVideoRecording()
                showToast("Video failed: ${error.message}")
            }
        )
    }

    private fun capturePhoto() {
        cameraKit.capturePhotoData(
            onSuccess = {
                viewModel.finishCapture()
                showToast("Photo captured")
            },
            onFailure = { error ->
                viewModel.finishCapture()
                showToast("Capture failed: ${error.message}")
            }
        )
    }

    private fun showTimerMenu() {
        val currentTimer = viewModel.uiState.value?.timerSeconds ?: 0
        PopupMenu(requireContext(), binding.btnTimerToggle, Gravity.NO_GRAVITY).apply {
            menu.add(0, 0, 0, "Timer Off")
            menu.add(0, 3, 1, "3 seconds")
            menu.add(0, 5, 2, "5 seconds")
            menu.add(0, 10, 3, "10 seconds")
            menu.setGroupCheckable(0, true, true)
            menu.findItem(currentTimer)?.isChecked = true
            setOnMenuItemClickListener {
                viewModel.setTimer(it.itemId)
                true
            }
        }.show()
    }

    private fun showAspectRatioMenu() {
        val currentRatio = viewModel.uiState.value?.aspectRatioLabel ?: "4:3"
        PopupMenu(requireContext(), binding.btnRatioConfig, Gravity.NO_GRAVITY).apply {
            menu.add(0, 43, 0, "4:3")
            menu.add(0, 169, 1, "16:9")
            menu.setGroupCheckable(0, true, true)
            menu.findItem(if (currentRatio == "16:9") 169 else 43)?.isChecked = true
            setOnMenuItemClickListener {
                val label = it.title.toString()
                cameraKit.setAspectRatio(if (label == "16:9") 1 else 0)
                viewModel.setAspectRatio(label)
                true
            }
        }.show()
    }

    private fun showResolutionSelector() {
        val dialogBinding = DialogResolutionSelectorBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        val adapter = ResolutionPresetAdapter { preset ->
            applyResolutionPreset(preset)
            dialog.dismiss()
        }
        val state = viewModel.uiState.value ?: HdCameraUiState()

        dialogBinding.rvResolutions.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvResolutions.adapter = adapter
        fun renderFilterSelection(selected: TextView) {
            listOf(dialogBinding.btnRatioAll, dialogBinding.btnRatio43, dialogBinding.btnRatio169)
                .forEach { chip ->
                    val active = chip == selected
                    chip.setBackgroundResource(
                        if (active) R.drawable.bg_camera_mode_active else R.drawable.bg_camera_resolution_filter
                    )
                    chip.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (active) R.color.white else R.color.text_dark_primary
                        )
                    )
                    chip.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
                }
        }

        fun submitFilteredResolutions(filter: String?) {
            val filtered = if (filter == null) {
                state.resolutionPresets
            } else {
                state.resolutionPresets.filter { it.ratioLabel == filter }
            }
            adapter.submitResolutions(filtered, viewModel.uiState.value?.selectedResolution)
        }

        renderFilterSelection(dialogBinding.btnRatioAll)
        submitFilteredResolutions(null)
        dialogBinding.btnCloseDialog.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnRatioAll.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatioAll)
            submitFilteredResolutions(null)
        }
        dialogBinding.btnRatio43.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatio43)
            submitFilteredResolutions("4:3")
        }
        dialogBinding.btnRatio169.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatio169)
            submitFilteredResolutions("16:9")
        }

        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun applyResolutionPreset(preset: ResolutionPreset) {
        viewModel.selectResolution(preset)
        cameraKit.setManualResolution(preset.width, preset.height)
    }

    private fun navigateSafely(actionId: Int) {
        runCatching {
            findNavController().navigate(actionId)
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatRecordingDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun formatRecordingPillDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d.%02d", minutes, seconds)
    }

    private data class FilterOption(
        val root: View,
        val card: MaterialCardView,
        val label: TextView,
        val filterName: String
    )

    private companion object {
        const val FILTER_DEFAULT = "Default"
        const val FILTER_FOGGY = "Foggy"
        const val FILTER_DAYLIGHT = "Daylight"
        const val FILTER_SPIKE = "Spike"
        const val FILTER_GLOAM = "Gloam"
    }
}
