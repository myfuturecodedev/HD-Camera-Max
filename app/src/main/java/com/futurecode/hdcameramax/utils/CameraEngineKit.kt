package com.futurecode.hdcameramax.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.futurecode.hdcameramax.model.CameraAppMode
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import java.util.Locale


//
//class CameraEngineKit(
//    private val context: Context,
//    private val lifecycleOwner: LifecycleOwner,
//    private val viewFinder: PreviewView
//) {
//    private var camera: Camera? = null
//    private var cameraProvider: ProcessCameraProvider? = null
//    private var preview: Preview? = null
//    private var imageCapture: ImageCapture? = null
//
//    private var activeMode = CameraAppMode.PHOTO
//    private var lensFacing = CameraSelector.LENS_FACING_BACK
//    private var flashMode = ImageCapture.FLASH_MODE_OFF
//    private var manualResolution: Size? = null
//    private var defaultAspectRatio = AspectRatio.RATIO_4_3
//
//    // ✅ FIXED PERFORMANCE: Backing fields approach use kiya hai jisse compiler 'private' keyword par kabhi crash nahi karega
//    private var _currentExposureIndex = 0
//    val currentExposureIndex: Int
//        get() = _currentExposureIndex
//
//    private var _currentWhiteBalance = "AUTO"
//    val currentWhiteBalance: String
//        get() = _currentWhiteBalance
//
//    private var _currentIsoValue = 400
//    val currentIsoValue: Int
//        get() = _currentIsoValue
//
//    fun initializePipeline(onReady: () -> Unit = {}) {
//        val providerFuture = ProcessCameraProvider.getInstance(context)
//        providerFuture.addListener({
//            cameraProvider = providerFuture.get()
//            rebuildCameraUseCasePipeline()
//            onReady()
//        }, ContextCompat.getMainExecutor(context))
//    }
//
//    fun switchAppMode(newMode: CameraAppMode) {
//        if (activeMode == newMode) return
//        activeMode = newMode
//        rebuildCameraUseCasePipeline()
//    }
//
//    fun switchLensFacing() {
//        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
//            CameraSelector.LENS_FACING_FRONT
//        } else {
//            CameraSelector.LENS_FACING_BACK
//        }
//        rebuildCameraUseCasePipeline()
//    }
//
//    fun cycleFlashMode(): Int {
//        flashMode = when (flashMode) {
//            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
//            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
//            else -> ImageCapture.FLASH_MODE_OFF
//        }
//        imageCapture?.flashMode = flashMode
//        return flashMode
//    }
//
//    fun applyZoomRatio(multiplier: Float) {
//        camera?.let { cam ->
//            val zoomState = cam.cameraInfo.zoomState.value
//            val max = zoomState?.maxZoomRatio ?: 100f
//            val min = zoomState?.minZoomRatio ?: 1f
//            val safeZoom = multiplier.coerceIn(min, max)
//            cam.cameraControl.setZoomRatio(safeZoom)
//        }
//    }
//
//    fun setExposureLevel(progressValue: Int): Float {
//        camera?.let { cam ->
//            val range = cam.cameraInfo.exposureState.exposureCompensationRange
//            if (range.contains(progressValue)) {
//                cam.cameraControl.setExposureCompensationIndex(progressValue)
//                _currentExposureIndex = progressValue
//            }
//        }
//        return _currentExposureIndex * (camera?.cameraInfo?.exposureState?.exposureCompensationStep?.toFloat() ?: 0.1f)
//    }
//
//    fun setManualResolution(width: Int, height: Int) {
//        manualResolution = Size(width, height)
//        rebuildCameraUseCasePipeline()
//    }
//
//    fun setAspectRatio(ratioType: Int) {
//        manualResolution = null
//        defaultAspectRatio = if (ratioType == 1) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3
//        rebuildCameraUseCasePipeline()
//    }
//
//    fun triggerManualFocus(x: Float, y: Float) {
//        val factory = viewFinder.meteringPointFactory
//        val point = factory.createPoint(x, y)
//        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
//            .setAutoCancelDuration(3, TimeUnit.SECONDS)
//            .build()
//        camera?.cameraControl?.startFocusAndMetering(action)
//    }
//
//    fun updateWhiteBalance(mode: String) {
//        _currentWhiteBalance = mode
//    }
//
//    private fun rebuildCameraUseCasePipeline() {
//        val provider = cameraProvider ?: return
//        provider.unbindAll()
//
//        val previewBuilder = Preview.Builder()
//        val captureBuilder = ImageCapture.Builder()
//            .setFlashMode(flashMode)
//            // FIXED: Removed the invalid .setTheme(null) line from here completely
//            .setCaptureMode(
//                if (activeMode == CameraAppMode.PORTRAIT)
//                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
//                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
//            )
//
//        manualResolution?.let { size ->
//            previewBuilder.setTargetResolution(size)
//            captureBuilder.setTargetResolution(size)
//        } ?: run {
//            previewBuilder.setTargetAspectRatio(defaultAspectRatio)
//            captureBuilder.setTargetAspectRatio(defaultAspectRatio)
//        }
//
//        preview = previewBuilder.build().also {
//            it.setSurfaceProvider(viewFinder.surfaceProvider)
//        }
//        imageCapture = captureBuilder.build()
//
//        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
//
//        try {
//            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
//            val autoPoint = viewFinder.meteringPointFactory.createPoint(viewFinder.width / 2f, viewFinder.height / 2f)
//            camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(autoPoint, FocusMeteringAction.FLAG_AF).disableAutoCancel().build())
//        } catch (e: Exception) {
//            Toast.makeText(context, "Pipeline crash protected: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    fun capturePhotoData(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
//        val capture = imageCapture ?: return
//        val name = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
//
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CameraHDMax")
//            }
//        }
//
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(
//            context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
//        ).build()
//
//        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    onSuccess(output.savedUri?.toString() ?: "Saved Successfully")
//                }
//                override fun onError(exc: ImageCaptureException) {
//                    onFailure(exc)
//                }
//            }
//        )
//    }
//}




class CameraEngineKit(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewFinder: PreviewView
) {

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    private var activeMode = CameraAppMode.PHOTO
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var manualResolution: Size? = null
    private var defaultAspectRatio = AspectRatio.RATIO_4_3

    private var _currentExposureIndex = 0

    fun initializePipeline(onReady: () -> Unit = {}) {
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            rebuildCameraUseCasePipeline()
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchAppMode(newMode: CameraAppMode) {
        if (activeMode == newMode) return

        activeMode = newMode
        rebuildCameraUseCasePipeline()
    }

    fun switchLensFacing() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        rebuildCameraUseCasePipeline()
    }

    fun cycleFlashMode(): Int {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }

        imageCapture?.flashMode = flashMode
        return flashMode
    }

    fun applyZoomRatio(multiplier: Float) {
        val cam = camera ?: return
        val zoomState = cam.cameraInfo.zoomState.value ?: return

        val safeZoom = multiplier.coerceIn(
            zoomState.minZoomRatio,
            zoomState.maxZoomRatio
        )

        cam.cameraControl.setZoomRatio(safeZoom)
    }

    fun setExposureLevel(progressValue: Int): Float {
        val cam = camera ?: return 0f

        val exposureState = cam.cameraInfo.exposureState
        val range = exposureState.exposureCompensationRange

        val safeIndex = progressValue.coerceIn(range.lower, range.upper)

        cam.cameraControl.setExposureCompensationIndex(safeIndex)
        _currentExposureIndex = safeIndex

        return safeIndex * exposureState.exposureCompensationStep.toFloat()
    }

    fun setManualResolution(width: Int, height: Int) {
        manualResolution = Size(width, height)
        rebuildCameraUseCasePipeline()
    }

    fun setAspectRatio(ratioType: Int) {
        manualResolution = null

        defaultAspectRatio = if (ratioType == 1) {
            AspectRatio.RATIO_16_9
        } else {
            AspectRatio.RATIO_4_3
        }

        rebuildCameraUseCasePipeline()
    }

    fun triggerManualFocus(x: Float, y: Float) {
        val point = viewFinder.meteringPointFactory.createPoint(x, y)

        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        camera?.cameraControl?.startFocusAndMetering(action)
    }

    private fun rebuildCameraUseCasePipeline() {
        val provider = cameraProvider ?: return

        try {
            provider.unbindAll()

            viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER

            val previewBuilder = Preview.Builder()
            val captureBuilder = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .setCaptureMode(
                    if (activeMode == CameraAppMode.PORTRAIT) {
                        ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    } else {
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    }
                )

            if (manualResolution != null) {
                previewBuilder.setTargetResolution(manualResolution!!)
                captureBuilder.setTargetResolution(manualResolution!!)
            } else {
                previewBuilder.setTargetAspectRatio(defaultAspectRatio)
                captureBuilder.setTargetAspectRatio(defaultAspectRatio)
            }

            preview = previewBuilder.build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = captureBuilder.build()

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture
            )

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Camera failed: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun capturePhotoData(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val capture = imageCapture ?: return

        val name = "IMG_" + SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CameraHDMax")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSuccess(output.savedUri?.toString() ?: "Saved Successfully")
                }

                override fun onError(exc: ImageCaptureException) {
                    onFailure(exc)
                }
            }
        )
    }
}