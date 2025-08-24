package com.example.cameraapp

import ControlsBar
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // State holders
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }

    // Rotation state
    var targetRotation by remember { mutableStateOf(Surface.ROTATION_0) }
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                targetRotation = when {
                    orientation in 45..134 -> Surface.ROTATION_270
                    orientation in 135..224 -> Surface.ROTATION_180
                    orientation in 225..314 -> Surface.ROTATION_90
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
    LaunchedEffect(lensFacing) {
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
