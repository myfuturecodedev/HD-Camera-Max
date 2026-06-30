package com.futurecode.hdcameramax.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private var activeMode = CameraAppMode.PHOTO
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var manualResolution: Size? = null
    private var defaultAspectRatio = AspectRatio.RATIO_4_3
    private var hdrQualityEnabled = false

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

    fun setFlashMode(mode: Int): Int {
        flashMode = when (mode) {
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = flashMode
        return flashMode
    }

    fun setHdrQualityEnabled(enabled: Boolean) {
        hdrQualityEnabled = enabled
        rebuildCameraUseCasePipeline()
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
            activeRecording?.stop()
            activeRecording = null
            provider.unbindAll()

            viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER

            val previewBuilder = Preview.Builder()
            val captureBuilder = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .setCaptureMode(
                    if (activeMode == CameraAppMode.PORTRAIT || hdrQualityEnabled) {
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
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            camera = try {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch (videoException: Exception) {
                videoCapture = null
                provider.unbindAll()
                preview = previewBuilder.build().also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
                imageCapture = captureBuilder.build()
                Toast.makeText(
                    context,
                    "Video mode is not supported on this device",
                    Toast.LENGTH_SHORT
                ).show()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
            }

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
        val capture = imageCapture ?: run {
            onFailure(IllegalStateException("Camera is not ready"))
            return
        }

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

    fun startVideoRecording(
        onStarted: () -> Unit,
        onFinalized: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val capture = videoCapture ?: run {
            onFailure(IllegalStateException("Video recorder is not ready"))
            return
        }

        if (activeRecording != null) {
            onFailure(IllegalStateException("Video recording is already running"))
            return
        }

        val name = "VID_" + SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/CameraHDMax")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        try {
            var pendingRecording = capture.output.prepareRecording(context, outputOptions)
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pendingRecording = pendingRecording.withAudioEnabled()
            }

            activeRecording = pendingRecording.start(
                ContextCompat.getMainExecutor(context)
            ) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onStarted()
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        if (event.hasError()) {
                            val cause = event.cause
                            onFailure(
                                if (cause is Exception) {
                                    cause
                                } else {
                                    IllegalStateException(
                                        "Video recording failed: ${event.error}",
                                        cause
                                    )
                                }
                            )
                        } else {
                            onFinalized(event.outputResults.outputUri.toString())
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            activeRecording = null
            onFailure(exception)
        }
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
    }

    fun release() {
        activeRecording?.stop()
        activeRecording = null
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        videoCapture = null
    }
}
