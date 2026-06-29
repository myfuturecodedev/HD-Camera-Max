package com.futurecode.hdcameramax.ui.afterlogin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView // FIXED: Added missing TextView import
import android.widget.Toast
import androidx.camera.core.ImageCapture
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentHdCameraBinding
import com.futurecode.hdcameramax.model.CameraAppMode
import com.futurecode.hdcameramax.utils.CameraEngineKit
import java.util.Locale // FIXED: Added missing Locale import
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

//class HdCameraFragment : BaseFragment<FragmentHdCameraBinding>(FragmentHdCameraBinding::inflate) {
//
//    private lateinit var cameraKit: CameraEngineKit
//    private var activeExposureMultiplier = 0
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // 1. Core Engine Hook Setup
//        cameraKit = CameraEngineKit(requireContext(), viewLifecycleOwner, binding.cameraViewFinder)
//        cameraKit.initializePipeline {
//            // Callback can track dynamic initial loading completions safely
//        }
//
//        // 2. Click Matrix Bindings Execution
//        setupCoreClickListeners()
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupCoreClickListeners() {
//
//        // Lens Flipper Trigger
//        binding.btnCameraFlip.setOnClickListener { cameraKit.switchLensFacing() }
//
//        // Flash State Looper Controller
//        binding.btnFlash.setOnClickListener {
//            val state = cameraKit.cycleFlashMode()
//            when (state) {
//                ImageCapture.FLASH_MODE_ON -> binding.btnFlash.setImageResource(R.drawable.ic_settings) // Replace to active asset
//                ImageCapture.FLASH_MODE_AUTO -> binding.btnFlash.setImageResource(R.drawable.ic_live_effects_filter)
//                else -> binding.btnFlash.setImageResource(R.drawable.ic_flash_off)
//            }
//        }
//
//        // Zoom Discrete Shortcuts mapping
//        binding.tvZoom1x.setOnClickListener { updateZoomLevel(1.0f, it) }
//        binding.tvZoom10x.setOnClickListener { updateZoomLevel(2.0f, it) }
//        binding.tvZoom100x.setOnClickListener { updateZoomLevel(5.0f, it) }
//
//        // Continuous Zoom Seek Slider integration
//        binding.sbDynamicZoomSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
//                if (fromUser) {
//                    val actualScale = 1.0f + (progress / 100f) * 10f // Scale logic up to 10x smoothly
//                    cameraKit.applyZoomRatio(actualScale)
//                    binding.tvZoomMaxCapIndicator.text = String.format(Locale.US, "%.0fx", actualScale)
//                }
//            }
//            override fun onStartTrackingTouch(p0: SeekBar?) {}
//            override fun onStopTrackingTouch(p0: SeekBar?) {}
//        })
//
//        // Touch-To-Focus Interaction Layer Mapping
//        binding.cameraViewFinder.setOnTouchListener { view, event ->
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                cameraKit.triggerManualFocus(event.x, event.y)
//                renderInteractiveFocusSquare(event.x, event.y)
//                view.performClick()
//                return@setOnTouchListener true
//            }
//            false
//        }
//
//        // Exposure Index Matrix Adjusters Modification
//        binding.btnExposurePlus.setOnClickListener {
//            if (activeExposureMultiplier < 4) {
//                activeExposureMultiplier++
//                applyExposureChange()
//            }
//        }
//        binding.btnExposureMinus.setOnClickListener {
//            if (activeExposureMultiplier > -4) {
//                activeExposureMultiplier--
//                applyExposureChange()
//            }
//        }
//
//        // FIXED: Explicitly passing exact layout binding properties to avoid 'R' type inference ambiguity
//        binding.tvTabVideoMode.setOnClickListener { switchUIMode(CameraAppMode.VIDEO, binding.tvTabVideoMode) }
//        binding.tvTabPhotoMode.setOnClickListener { switchUIMode(CameraAppMode.PHOTO, binding.tvTabPhotoMode) }
//        binding.tvTabPortraitMode.setOnClickListener { switchUIMode(CameraAppMode.PORTRAIT, binding.tvTabPortraitMode) }
//
//
//        // High Definition Real-time Shutter Action Trigger Button
//        binding.ivActionShutter.setOnClickListener {
//            binding.ivActionShutter.isEnabled = false
//            cameraKit.capturePhotoData(
//                onSuccess = { path ->
//                    binding.ivActionShutter.isEnabled = true
//                    Toast.makeText(context, "Captured: $path", Toast.LENGTH_SHORT).show()
//                },
//                onFailure = { err ->
//                    binding.ivActionShutter.isEnabled = true
//                    Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_SHORT).show()
//                }
//            )
//        }
//    }
//
//    private fun updateZoomLevel(scale: Float, view: View) {
//        cameraKit.applyZoomRatio(scale)
//        binding.tvZoom1x.setBackgroundResource(R.drawable.bg_circle_icon)
//        binding.tvZoom10x.setBackgroundResource(R.drawable.bg_circle_icon)
//        binding.tvZoom100x.setBackgroundResource(R.drawable.bg_circle_icon)
//
//        view.setBackgroundResource(R.drawable.bg_selected_check) // Toggle highlight visual representation state
//        binding.tvZoomMaxCapIndicator.text = "${scale.toInt()}x"
//        binding.sbDynamicZoomSeeker.progress = ((scale - 1f) / 10f * 100).toInt()
//    }
//
//    private fun applyExposureChange() {
//        val displayCalc = cameraKit.setExposureLevel(activeExposureMultiplier)
//        binding.tvExposureVal.text = String.format(Locale.US, "%.1f", displayCalc)
//    }
//
//    private fun switchUIMode(mode: CameraAppMode, activeTargetView: TextView) {
//        cameraKit.switchAppMode(mode)
//
//        // Reset Text UI highlighting colors arrays properties
//        binding.tvTabVideoMode.setTypeface(null, android.graphics.Typeface.NORMAL)
//        binding.tvTabPhotoMode.setTypeface(null, android.graphics.Typeface.NORMAL)
//        binding.tvTabPortraitMode.setTypeface(null, android.graphics.Typeface.NORMAL)
//
//        activeTargetView.setTypeface(null, android.graphics.Typeface.BOLD)
//        Toast.makeText(context, "Switched to ${mode.name} Mode", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun renderInteractiveFocusSquare(x: Float, y: Float) {
//        binding.viewFocusFrameIndicator.x = x - (binding.viewFocusFrameIndicator.width / 2f)
//        binding.viewFocusFrameIndicator.y = y - (binding.viewFocusFrameIndicator.height / 2f)
//        binding.viewFocusFrameIndicator.visibility = View.VISIBLE
//
//        // FIXED: Replaced '0dp.toFloat()' with clean '0f' for alpha value
//        binding.viewFocusFrameIndicator.animate().alpha(1f).setDuration(100).withEndAction {
//            binding.viewFocusFrameIndicator.animate().alpha(0f).setStartDelay(2000).setDuration(500).start()
//        }.start()
//    }
//}






class HdCameraFragment : BaseFragment<FragmentHdCameraBinding>(FragmentHdCameraBinding::inflate) {

    private lateinit var cameraKit: CameraEngineKit
    private var activeExposureMultiplier = 0

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraPreview()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraKit = CameraEngineKit(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            viewFinder = binding.cameraViewFinder
        )

        setupCoreClickListeners()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startCameraPreview()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraPreview() {
        cameraKit.initializePipeline {
            Toast.makeText(requireContext(), "Camera ready", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCoreClickListeners() {

        binding.btnHeaderBackAction.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCameraFlip.setOnClickListener {
            cameraKit.switchLensFacing()
        }

        binding.btnFlash.setOnClickListener {
            val state = cameraKit.cycleFlashMode()

            when (state) {
                ImageCapture.FLASH_MODE_ON -> {
                    binding.btnFlash.setImageResource(R.drawable.ic_flash)
                }

                ImageCapture.FLASH_MODE_AUTO -> {
                    binding.btnFlash.setImageResource(R.drawable.ic_live_effects_filter)
                }

                else -> {
                    binding.btnFlash.setImageResource(R.drawable.ic_flash_off)
                }
            }
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

        binding.sbDynamicZoomSeeker.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val actualScale = 1.0f + (progress / 100f) * 9f
                        cameraKit.applyZoomRatio(actualScale)
                        binding.tvZoomMaxCapIndicator.text =
                            String.format(Locale.US, "%.1fx", actualScale)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}

                override fun onStopTrackingTouch(sb: SeekBar?) {}
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
            if (activeExposureMultiplier < 4) {
                activeExposureMultiplier++
                applyExposureChange()
            }
        }

        binding.btnExposureMinus.setOnClickListener {
            if (activeExposureMultiplier > -4) {
                activeExposureMultiplier--
                applyExposureChange()
            }
        }

        binding.tvTabVideoMode.setOnClickListener {
            switchUIMode(CameraAppMode.VIDEO, binding.tvTabVideoMode)
        }

        binding.tvTabPhotoMode.setOnClickListener {
            switchUIMode(CameraAppMode.PHOTO, binding.tvTabPhotoMode)
        }

        binding.tvTabPortraitMode.setOnClickListener {
            switchUIMode(CameraAppMode.PORTRAIT, binding.tvTabPortraitMode)
        }

        binding.ivActionShutter.setOnClickListener {
            binding.ivActionShutter.isEnabled = false

            cameraKit.capturePhotoData(
                onSuccess = { path ->
                    binding.ivActionShutter.isEnabled = true
                    Toast.makeText(requireContext(), "Captured: $path", Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    binding.ivActionShutter.isEnabled = true
                    Toast.makeText(requireContext(), "Failed: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun updateZoomLevel(scale: Float, view: View) {
        cameraKit.applyZoomRatio(scale)

        binding.tvZoom1x.setBackgroundResource(R.drawable.bg_circle_icon)
        binding.tvZoom10x.setBackgroundResource(R.drawable.bg_circle_icon)
        binding.tvZoom100x.setBackgroundResource(R.drawable.bg_circle_icon)

        view.setBackgroundResource(R.drawable.bg_selected_check)

        binding.tvZoomMaxCapIndicator.text = "${scale.toInt()}x"
        binding.sbDynamicZoomSeeker.progress = (((scale - 1f) / 9f) * 100).toInt()
    }

    private fun applyExposureChange() {
        val exposureValue = cameraKit.setExposureLevel(activeExposureMultiplier)
        binding.tvExposureVal.text = String.format(Locale.US, "%.1f", exposureValue)
    }

    private fun switchUIMode(mode: CameraAppMode, activeTargetView: TextView) {
        cameraKit.switchAppMode(mode)

        binding.tvTabVideoMode.setTypeface(null, android.graphics.Typeface.NORMAL)
        binding.tvTabPhotoMode.setTypeface(null, android.graphics.Typeface.NORMAL)
        binding.tvTabPortraitMode.setTypeface(null, android.graphics.Typeface.NORMAL)

        activeTargetView.setTypeface(null, android.graphics.Typeface.BOLD)

        Toast.makeText(requireContext(), "Switched to ${mode.name} Mode", Toast.LENGTH_SHORT).show()
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
}