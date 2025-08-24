package com.example.cameraX

import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // State holders
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    // Rotation state
    var targetRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                targetRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = targetRotation
                videoCapture?.targetRotation = targetRotation
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }

    // PreviewView (camera preview UI)
    val previewView = remember { PreviewView(context) }

    // Bind/unbind camera whenever lens changes
//    LaunchedEffect(lensFacing) {
//        scope.launch(Dispatchers.Main) {
//            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//            val preview = Preview.Builder()
//                .setTargetRotation(targetRotation)
//                .build()
//            val imgCapture = ImageCapture.Builder()
//                .setFlashMode(flashMode)
//                .setTargetRotation(targetRotation)
//                .build()
//            val recorder = Recorder.Builder()
//                .setQualitySelector(QualitySelector.from(Quality.FHD))
//                .build()
//            val vidCapture = VideoCapture.withOutput(recorder)
//
//            try {
//                cameraProvider.unbindAll()
//                val selector = CameraSelector.Builder()
//                    .requireLensFacing(lensFacing)
//                    .build()
//
//                val cam = cameraProvider.bindToLifecycle(
//                    lifecycleOwner,
//                    selector,
//                    preview,
//                    imgCapture,
//                    vidCapture
//                )
//
//                preview.surfaceProvider = previewView.surfaceProvider
//
//                imageCapture = imgCapture
//                videoCapture = vidCapture
//                camera = cam
//
//                cam.cameraControl.enableTorch(torchEnabled)
//
//            } catch (e: Exception) {
//                Log.e("CameraApp", "Binding failed", e)
//            }
//        }
//    }

    LaunchedEffect(lensFacing, targetRotation) {
        scope.launch(Dispatchers.Main) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()

            val preview = Preview.Builder()
                .setTargetRotation(targetRotation)
                .build()

            val imgCapture = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .setTargetRotation(targetRotation)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()

            val vidCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imgCapture,
                    vidCapture
                )

                // ✅ THIS IS THE IMPORTANT PART
                preview.setSurfaceProvider(previewView.surfaceProvider)

                imageCapture = imgCapture
                videoCapture = vidCapture
                camera = cam

                cam.cameraControl.enableTorch(torchEnabled)

            } catch (e: Exception) {
                Log.e("CameraApp", "Binding failed", e)
            }
        }
    }


    // UI
    Scaffold(
        bottomBar = {
            ControlsBar(
                onTakePhoto = {
                    imageCapture?.let { capture ->
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(
                            context.cacheDir.resolve("photo_${System.currentTimeMillis()}.jpg")
                        ).build()
                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    Log.d("CameraApp", "Photo saved: ${outputFileResults.savedUri}")
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Log.e("CameraApp", "Photo capture failed: ${exc.message}", exc)
                                }
                            }
                        )
                    }
                },
                onToggleVideo = {
                    if (activeRecording != null) {
                        activeRecording?.stop()
                        activeRecording = null
                    } else {
                        videoCapture?.let { vidCap ->
                            val file = context.cacheDir.resolve("video_${System.currentTimeMillis()}.mp4")
                            val outputOptions = FileOutputOptions.Builder(file).build()
                            activeRecording = vidCap.output
                                .prepareRecording(context, outputOptions)
                                .start(ContextCompat.getMainExecutor(context)) {
                                    Log.d("CameraApp", "Recording event: $it")
                                }
                        }
                    }
                },
                onSwitchCamera = {
                    lensFacing =
                        if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                        else CameraSelector.LENS_FACING_BACK
                },
                onToggleFlash = {
                    torchEnabled = !torchEnabled
                    camera?.cameraControl?.enableTorch(torchEnabled)
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { previewView }
        )
    }
}
