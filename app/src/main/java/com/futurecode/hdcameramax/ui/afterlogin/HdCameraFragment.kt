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
import androidx.constraintlayout.widget.ConstraintLayout
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
    private var dashboardFeature: String? = null

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
        dashboardFeature = arguments?.getString(ARG_DASHBOARD_FEATURE)

        setupCoreClickListeners()
        observeCameraState()
        applyDashboardFeature(applyCameraControls = false)
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
            applyDashboardFeature(applyCameraControls = true)
            if (startVideoAfterCameraReady) {
                startVideoAfterCameraReady = false
                handleVideoRecordingRequest()
            }
        }
    }

    private fun applyDashboardFeature(applyCameraControls: Boolean) {
        when (dashboardFeature) {
            FEATURE_HD_ZOOM -> {
                cameraKit.switchAppMode(CameraAppMode.PHOTO)
                viewModel.setMode(CameraAppMode.PHOTO)
                viewModel.updateZoom(5.0f)
                binding.sbDynamicZoomSeeker.progress = (((5.0f - 1f) / 9f) * 100).toInt()
                if (applyCameraControls) {
                    cameraKit.applyZoomRatio(5.0f)
                }
            }

            FEATURE_PORTRAIT -> {
                cameraKit.switchAppMode(CameraAppMode.PORTRAIT)
                viewModel.setMode(CameraAppMode.PORTRAIT)
            }

            FEATURE_FILTERS -> {
                cameraKit.switchAppMode(CameraAppMode.PORTRAIT)
                viewModel.setMode(CameraAppMode.PORTRAIT)
                viewModel.setFilter(FILTER_DEFAULT)
            }

            FEATURE_BEAUTY -> {
                cameraKit.switchAppMode(CameraAppMode.PORTRAIT)
                viewModel.setMode(CameraAppMode.PORTRAIT)
                viewModel.selectFocusMode(FOCUS_MACRO)
                viewModel.setFilter(FILTER_DAYLIGHT)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCoreClickListeners() {
        binding.btnHeaderBackAction.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnRatioConfig.setOnClickListener {
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
            viewModel.toggleTimerStrip()
        }

//        binding.btnGridToggle.setOnClickListener {
//            viewModel.toggleGrid()
//        }

        binding.btnSettings.setOnClickListener {
            showAspectRatioMenu()
        }

        binding.btnPanelGrid.setOnClickListener {
            viewModel.toggleGrid()
        }

        binding.tvCompositionDone.setOnClickListener {
            viewModel.hideGridSelector()
        }

        binding.gridGuideNone.setOnClickListener {
            viewModel.selectGridGuide(GRID_NONE)
        }

        binding.gridGuide3x3.setOnClickListener {
            viewModel.selectGridGuide(GRID_3X3)
        }

        binding.gridGuide4x2.setOnClickListener {
            viewModel.selectGridGuide(GRID_4X2)
        }

        binding.gridGuideCross.setOnClickListener {
            viewModel.selectGridGuide(GRID_CROSS)
        }

        binding.gridGuideGolden.setOnClickListener {
            viewModel.selectGridGuide(GRID_GOLDEN)
        }

        binding.btnPanelHdr.setOnClickListener {
            showResolutionSelector()
            viewModel.hideTransientTopPanels()
        }

        binding.btnPanelFace.setOnClickListener {
            viewModel.toggleFocusSelector()
        }

        binding.btnFocusModeClose.setOnClickListener {
            viewModel.hideFocusSelector()
        }

        binding.focusModeAuto.setOnClickListener {
            viewModel.selectFocusMode(FOCUS_AUTO)
        }

        binding.focusModeMacro.setOnClickListener {
            viewModel.selectFocusMode(FOCUS_MACRO)
        }

        binding.focusModeCenter.setOnClickListener {
            viewModel.selectFocusMode(FOCUS_CENTER)
        }

        binding.focusModeTracking.setOnClickListener {
            viewModel.selectFocusMode(FOCUS_TRACKING)
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

        binding.wbControlColumn.setOnClickListener {
            viewModel.toggleWhiteBalanceStrip()
        }

        binding.wbIncandescentOption.setOnClickListener {
            viewModel.setWhiteBalance(WB_INCANDESCENT)
        }

        binding.wbFluorescentOption.setOnClickListener {
            viewModel.setWhiteBalance(WB_FLUORESCENT)
        }

        binding.wbDaylightOption.setOnClickListener {
            viewModel.setWhiteBalance(WB_DAYLIGHT)
        }

        binding.wbCloudyOption.setOnClickListener {
            viewModel.setWhiteBalance(WB_CLOUDY)
        }

        binding.wbShadeOption.setOnClickListener {
            viewModel.setWhiteBalance(WB_SHADE)
        }

        binding.btnPanelFull.setOnClickListener {
            viewModel.showAspectSelector()
        }

        binding.tvAspect169.setOnClickListener {
            applyAspectLabel("16:9")
        }

        binding.tvAspect11.setOnClickListener {
            applyAspectLabel("1:1")
        }

        binding.tvAspect43.setOnClickListener {
            applyAspectLabel("4:3")
        }

        binding.tvAspectFull.setOnClickListener {
            applyAspectLabel("Full")
        }

        binding.tvTimerOff.setOnClickListener {
            viewModel.setTimer(0)
        }

        binding.tvTimer3s.setOnClickListener {
            viewModel.setTimer(3)
        }

        binding.tvTimer5s.setOnClickListener {
            viewModel.setTimer(5)
        }

        binding.tvTimer10s.setOnClickListener {
            viewModel.setTimer(10)
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
                val focusMode = viewModel.uiState.value?.selectedFocusMode ?: FOCUS_MACRO
                if (focusMode != FOCUS_CENTER) {
                    cameraKit.triggerManualFocus(event.x, event.y)
                    renderInteractiveFocusSquare(event.x, event.y)
                }
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
            viewModel.hideTransientTopPanels()
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

    private fun applyAspectLabel(label: String) {
        val cameraAspect = if (label == "16:9") 1 else 0
        cameraKit.setAspectRatio(cameraAspect)
        viewModel.setAspectRatio(label)
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
        val showTimerStrip = state.isTimerStripVisible && !isRecordingVideo
        val showWhiteBalanceStrip = state.isWhiteBalanceStripVisible && !isRecordingVideo

        binding.viewGridLines.visibility = if (state.isGridEnabled) View.VISIBLE else View.GONE
        renderGridGuidePreview(state)
        renderFocusGuidePreview(state)
        binding.cameraControlsScrim.visibility =
            if (showSettingsPanel) View.VISIBLE else View.GONE
        binding.cameraSettingsPanel.visibility =
            if (showSettingsPanel) View.VISIBLE else View.GONE
        binding.cardCompositionGridSelector.visibility =
            if (state.isGridSelectorVisible && !isRecordingVideo) View.VISIBLE else View.GONE
        binding.cardFocusModeSelector.visibility =
            if (state.isFocusSelectorVisible && !isRecordingVideo) View.VISIBLE else View.GONE
        binding.llTimerQuickStrip.visibility =
            if (showTimerStrip) View.VISIBLE else View.GONE
        binding.tvCountdownOverlay.visibility =
            if (state.countdownValue > 0) View.VISIBLE else View.GONE
        binding.topBarHeaderControls.visibility = if (isRecordingVideo) View.GONE else View.VISIBLE
        binding.llStatusBadges.visibility = View.GONE
        binding.llInteractiveControlDeck.visibility = if (isRecordingVideo) View.GONE else View.VISIBLE
        binding.panelZoomSliderControl.visibility =
            if (showWhiteBalanceStrip) View.GONE else View.VISIBLE
        binding.llWhiteBalanceStrip.visibility =
            if (showWhiteBalanceStrip) View.VISIBLE else View.GONE
        binding.llFilterSelectorStrip.visibility =
            if (!isRecordingVideo && !showWhiteBalanceStrip && state.activeMode == CameraAppMode.PORTRAIT) View.VISIBLE else View.GONE
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
        binding.tvWhiteBalanceValue.text = state.selectedWhiteBalance
        binding.wbControlColumn.setBackgroundResource(
            if (showWhiteBalanceStrip || state.selectedWhiteBalance != WB_AUTO) {
                R.drawable.bg_camera_feature_tile_active
            } else {
                R.drawable.bg_camera_feature_tile_inactive
            }
        )
        binding.tvHdrBadge.text = if (state.isHdrEnabled) "ON" else "OFF"

        renderFlashIcon(state.flashMode)
        renderFeaturePanelState(state)
        renderCompositionGridSelectorState(state.selectedGridGuide)
        renderFocusModeSelectorState(state.selectedFocusMode)
        renderSettingsPanelLabels(state)
        renderStatusBadgeState(state)
        renderZoomChips(state.zoomRatio)
        renderFilterState(state.selectedFilter)
        renderWhiteBalanceState(state.selectedWhiteBalance)
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

    private fun renderSettingsPanelLabels(state: HdCameraUiState) {
        binding.tvPanelGridLabel.text = getString(R.string.grid)
        binding.tvPanelFocusLabel.text = getString(R.string.focus)
        binding.tvPanelHdrLabel.text = getString(R.string.hd_set)
        binding.tvPanelFullLabel.text = getString(R.string.full)
    }

    private fun renderFeaturePanelState(state: HdCameraUiState) {
        renderFeatureTile(
            binding.btnPanelGrid,
            listOf(binding.tvPanelGridIcon, binding.tvPanelGridLabel),
            state.isGridEnabled || state.isGridSelectorVisible
        )
        renderFeatureTile(
            binding.btnPanelFace,
            listOf(binding.tvPanelFocusIcon, binding.tvPanelFocusLabel),
            state.isFocusSelectorVisible || state.selectedFocusMode != FOCUS_AUTO
        )
        renderFeatureTile(
            binding.btnPanelHdr,
            listOf(binding.tvPanelHdrLabel),
            state.selectedResolution != null
        )
        binding.tvPanelHdrIcon.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (state.selectedResolution != null) R.color.black else R.color.white
            )
        )
        binding.tvPanelHdrIcon.setBackgroundResource(
            if (state.selectedResolution != null) R.drawable.bg_camera_top_chip_active else R.drawable.bg_camera_top_chip_inactive
        )
        renderFeatureTile(
            binding.btnPanelFull,
            listOf(binding.tvPanelFullIcon, binding.tvPanelFullLabel),
            state.isSettingsPanelVisible && !state.isGridSelectorVisible && !state.isFocusSelectorVisible
        )
        renderAspectChips(state.aspectRatioLabel)
        renderTimerChips(state.timerSeconds)
    }

    private fun renderGridGuidePreview(state: HdCameraUiState) {
        val guide = state.selectedGridGuide
        val showGuide = state.isGridEnabled && guide != GRID_NONE
        binding.viewGridLines.visibility = if (showGuide) View.VISIBLE else View.GONE
        if (!showGuide) return

        setHorizontalBias(binding.viewGridVerticalOne, if (guide == GRID_GOLDEN) 0.382f else 0.333f)
        setHorizontalBias(binding.viewGridVerticalTwo, if (guide == GRID_GOLDEN) 0.618f else 0.666f)
        setVerticalBias(binding.viewGridHorizontalOne, if (guide == GRID_GOLDEN) 0.382f else 0.333f)
        setVerticalBias(binding.viewGridHorizontalTwo, if (guide == GRID_GOLDEN) 0.618f else 0.666f)

        val isThreeByThree = guide == GRID_3X3 || guide == GRID_GOLDEN
        val isFourByTwo = guide == GRID_4X2
        val isCross = guide == GRID_CROSS

        binding.viewGridVerticalOne.visibility =
            if (isThreeByThree || isFourByTwo) View.VISIBLE else View.GONE
        binding.viewGridVerticalTwo.visibility =
            if (isThreeByThree || isFourByTwo) View.VISIBLE else View.GONE
        binding.viewGridHorizontalOne.visibility =
            if (isThreeByThree) View.VISIBLE else View.GONE
        binding.viewGridHorizontalTwo.visibility =
            if (isThreeByThree) View.VISIBLE else View.GONE
        binding.viewGridCrossVertical.visibility =
            if (isCross || isFourByTwo) View.VISIBLE else View.GONE
        binding.viewGridCrossHorizontal.visibility =
            if (isCross || isFourByTwo) View.VISIBLE else View.GONE
    }

    private fun setHorizontalBias(view: View, bias: Float) {
        val params = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.horizontalBias = bias
        view.layoutParams = params
    }

    private fun setVerticalBias(view: View, bias: Float) {
        val params = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.verticalBias = bias
        view.layoutParams = params
    }

    private fun renderCompositionGridSelectorState(selectedGuide: String) {
        renderGridGuideOption(
            binding.gridGuideNone,
            listOf(binding.tvGridGuideNoneIcon, binding.tvGridGuideNone),
            selectedGuide == GRID_NONE
        )
        renderGridGuideOption(
            binding.gridGuide3x3,
            listOf(binding.tvGridGuide3x3Icon, binding.tvGridGuide3x3),
            selectedGuide == GRID_3X3
        )
        renderGridGuideOption(
            binding.gridGuide4x2,
            listOf(binding.tvGridGuide4x2Icon, binding.tvGridGuide4x2),
            selectedGuide == GRID_4X2
        )
        renderGridGuideOption(
            binding.gridGuideCross,
            listOf(binding.tvGridGuideCrossIcon, binding.tvGridGuideCross),
            selectedGuide == GRID_CROSS
        )
        renderGridGuideOption(
            binding.gridGuideGolden,
            listOf(binding.tvGridGuideGoldenIcon, binding.tvGridGuideGolden),
            selectedGuide == GRID_GOLDEN
        )
    }

    private fun renderFocusGuidePreview(state: HdCameraUiState) {
        val focusMode = state.selectedFocusMode
        val showFaceFrame = state.isFaceOverlayEnabled && (focusMode == FOCUS_TRACKING || focusMode == FOCUS_MACRO)
        val showCenterFrame = focusMode == FOCUS_CENTER

        binding.viewFaceDetectionOverlay.visibility = if (showFaceFrame) View.VISIBLE else View.GONE
        binding.viewFocusFrameIndicator.clearAnimation()
        if (showCenterFrame) {
            binding.viewFocusFrameIndicator.alpha = 1f
            binding.viewFocusFrameIndicator.visibility = View.VISIBLE
            binding.viewFocusFrameIndicator.x =
                (binding.cameraViewFinder.width - binding.viewFocusFrameIndicator.width) / 2f
            binding.viewFocusFrameIndicator.y =
                (binding.cameraViewFinder.height - binding.viewFocusFrameIndicator.height) / 2f
        } else if (binding.viewFocusFrameIndicator.alpha >= 1f && binding.viewFocusFrameIndicator.visibility == View.VISIBLE) {
            binding.viewFocusFrameIndicator.visibility = View.GONE
        }
    }

    private fun renderFocusModeSelectorState(selectedFocusMode: String) {
        renderFocusModeOption(
            binding.focusModeAuto,
            binding.tvFocusAutoIcon,
            binding.tvFocusAutoLabel,
            selectedFocusMode == FOCUS_AUTO
        )
        renderFocusModeOption(
            binding.focusModeMacro,
            binding.tvFocusMacroIcon,
            binding.tvFocusMacroLabel,
            selectedFocusMode == FOCUS_MACRO
        )
        renderFocusModeOption(
            binding.focusModeCenter,
            binding.tvFocusCenterIcon,
            binding.tvFocusCenterLabel,
            selectedFocusMode == FOCUS_CENTER
        )
        renderFocusModeOption(
            binding.focusModeTracking,
            binding.tvFocusTrackingIcon,
            binding.tvFocusTrackingLabel,
            selectedFocusMode == FOCUS_TRACKING
        )
    }

    private fun renderFocusModeOption(root: View, icon: TextView, label: TextView, selected: Boolean) {
        root.setBackgroundResource(
            if (selected) R.drawable.bg_camera_feature_tile_active else R.drawable.bg_camera_feature_tile_inactive
        )
        icon.setBackgroundResource(
            if (selected) R.drawable.bg_focus_mode_circle_active else R.drawable.bg_focus_mode_circle_inactive
        )
        icon.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        label.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.permission_green else R.color.white
            )
        )
        label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun renderGridGuideOption(root: View, labels: List<TextView>, selected: Boolean) {
        root.setBackgroundResource(
            if (selected) R.drawable.bg_camera_feature_tile_active else R.drawable.bg_camera_feature_tile_inactive
        )
        labels.forEachIndexed { index, label ->
            val color = when {
                selected && index == 0 -> ContextCompat.getColor(requireContext(), R.color.permission_green)
                selected -> ContextCompat.getColor(requireContext(), R.color.white)
                else -> Color.parseColor("#C7CBD1")
            }
            label.setTextColor(color)
            label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun renderFeatureTile(root: View, labels: List<TextView>, selected: Boolean) {
        root.setBackgroundResource(
            if (selected) R.drawable.bg_camera_feature_tile_active else R.drawable.bg_camera_feature_tile_inactive
        )
        labels.forEach { label ->
            label.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.permission_green else R.color.white
                )
            )
            label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun renderAspectChips(selectedLabel: String) {
        val chips = listOf(
            binding.tvAspect169 to "16:9",
            binding.tvAspect11 to "1:1",
            binding.tvAspect43 to "4:3",
            binding.tvAspectFull to "Full"
        )
        chips.forEach { (chip, label) ->
            val selected = label == selectedLabel
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_camera_top_chip_active else R.drawable.bg_camera_top_chip_inactive
            )
            chip.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun renderTimerChips(timerSeconds: Int) {
        val chips = listOf(
            binding.tvTimerOff to 0,
            binding.tvTimer3s to 3,
            binding.tvTimer5s to 5,
            binding.tvTimer10s to 10
        )
        chips.forEach { (chip, value) ->
            val selected = value == timerSeconds
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_camera_top_chip_active else R.drawable.bg_camera_top_chip_inactive
            )
            chip.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
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

    private fun renderWhiteBalanceState(selectedWhiteBalance: String) {
        val whiteBalanceOptions = listOf(
            WhiteBalanceOption(
                binding.wbIncandescentOption,
                binding.cardWbIncandescent,
                binding.tvWbIncandescent,
                WB_INCANDESCENT
            ),
            WhiteBalanceOption(
                binding.wbFluorescentOption,
                binding.cardWbFluorescent,
                binding.tvWbFluorescent,
                WB_FLUORESCENT
            ),
            WhiteBalanceOption(
                binding.wbDaylightOption,
                binding.cardWbDaylight,
                binding.tvWbDaylight,
                WB_DAYLIGHT
            ),
            WhiteBalanceOption(
                binding.wbCloudyOption,
                binding.cardWbCloudy,
                binding.tvWbCloudy,
                WB_CLOUDY
            ),
            WhiteBalanceOption(
                binding.wbShadeOption,
                binding.cardWbShade,
                binding.tvWbShade,
                WB_SHADE
            )
        )

        whiteBalanceOptions.forEach { option ->
            val selected = option.whiteBalanceName == selectedWhiteBalance
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

        val overlayColor = when (selectedWhiteBalance) {
            WB_INCANDESCENT -> Color.parseColor("#44FFB15A")
            WB_FLUORESCENT -> Color.parseColor("#33DDE7F0")
            WB_DAYLIGHT -> Color.parseColor("#22FFD36A")
            WB_CLOUDY -> Color.parseColor("#443B4A66")
            WB_SHADE -> Color.parseColor("#55301954")
            else -> Color.TRANSPARENT
        }
        val overlayAlpha = when (selectedWhiteBalance) {
            WB_INCANDESCENT -> 0.20f
            WB_FLUORESCENT -> 0.18f
            WB_DAYLIGHT -> 0.14f
            WB_CLOUDY -> 0.24f
            WB_SHADE -> 0.28f
            else -> 0f
        }

        binding.viewWhiteBalanceOverlay.setBackgroundColor(overlayColor)
        binding.viewWhiteBalanceOverlay.animate()
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
        PopupMenu(requireContext(), binding.btnSettings, Gravity.NO_GRAVITY).apply {
            menu.add(0, 169, 0, "16:9")
            menu.add(0, 11, 1, "1:1")
            menu.add(0, 43, 2, "4:3")
            menu.add(0, 100, 3, "Full")
            menu.setGroupCheckable(0, true, true)
            menu.findItem(
                when (currentRatio) {
                    "16:9" -> 169
                    "1:1" -> 11
                    "Full" -> 100
                    else -> 43
                }
            )?.isChecked = true
            setOnMenuItemClickListener {
                applyAspectLabel(it.title.toString())
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
            listOf(
                dialogBinding.btnRatioAll,
                dialogBinding.btnRatio43,
                dialogBinding.btnRatio169,
                dialogBinding.btnRatio11,
                dialogBinding.btnRatioRecommended
            )
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
            val filtered = when (filter) {
                null -> state.resolutionPresets
                "Recommended" -> state.resolutionPresets.filter { it.isRecommended }
                else -> state.resolutionPresets.filter { it.ratioLabel == filter }
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
        dialogBinding.btnRatio11.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatio11)
            submitFilteredResolutions("1:1")
        }
        dialogBinding.btnRatioRecommended.setOnClickListener {
            renderFilterSelection(dialogBinding.btnRatioRecommended)
            submitFilteredResolutions("Recommended")
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

    private data class WhiteBalanceOption(
        val root: View,
        val card: MaterialCardView,
        val label: TextView,
        val whiteBalanceName: String
    )

    companion object {
        const val ARG_DASHBOARD_FEATURE = "dashboard_feature"
        const val FEATURE_HD_ZOOM = "hd_zoom"
        const val FEATURE_PORTRAIT = "portrait"
        const val FEATURE_FILTERS = "filters"
        const val FEATURE_BEAUTY = "beauty"

        const val FILTER_DEFAULT = "Default"
        const val FILTER_FOGGY = "Foggy"
        const val FILTER_DAYLIGHT = "Daylight"
        const val FILTER_SPIKE = "Spike"
        const val FILTER_GLOAM = "Gloam"
        const val WB_AUTO = "Auto"
        const val WB_INCANDESCENT = "Incandescent"
        const val WB_FLUORESCENT = "Fluorescent"
        const val WB_DAYLIGHT = "Daylight"
        const val WB_CLOUDY = "Cloudy"
        const val WB_SHADE = "Shade"
        const val GRID_NONE = "None"
        const val GRID_3X3 = "3×3"
        const val GRID_4X2 = "4×2"
        const val GRID_CROSS = "Cross"
        const val GRID_GOLDEN = "GR.2"
        const val FOCUS_AUTO = "Auto Focus"
        const val FOCUS_MACRO = "Macro Focus"
        const val FOCUS_CENTER = "Center Focus"
        const val FOCUS_TRACKING = "Tracking Focus"
    }
}
