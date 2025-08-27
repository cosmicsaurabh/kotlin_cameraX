package com.example.cameraX

import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Composable that shows the camera preview and control bar
 */
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // State variables for CameraX use cases
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    // Track device rotation and update capture targets
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

    // PreviewView (native Android camera preview inside Compose)
    val previewView = remember { PreviewView(context) }

    // Bind camera use cases whenever lens or rotation changes
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
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            val vidCapture = VideoCapture.withOutput(recorder)

            try {
                // Rebind all use cases
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

                // Setup global controller
                CameraXController.setup(
                    imgCapture,
                    vidCapture,
                    context
                ) { filePath ->
                    Log.d("CameraApp", "Media captured: $filePath")
                }

                // Attach preview surface
                preview.setSurfaceProvider(previewView.surfaceProvider)

                // Update state
                imageCapture = imgCapture
                videoCapture = vidCapture
                camera = cam

                cam.cameraControl.enableTorch(torchEnabled)

            } catch (e: Exception) {
                Log.e("CameraApp", "Binding failed", e)
            }
        }
    }

    // UI with camera preview + controls bar
    Scaffold(
        bottomBar = {
            ControlsBar(
                onTakePhoto = { CameraXController.takePhoto() },
                onToggleVideo = { isRecording ->
                    if (isRecording) CameraXController.startVideo()
                    else CameraXController.stopVideo()
                },
                onSwitchCamera = {
                    lensFacing =
                        if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                },
                onToggleFlash = { enabled ->
                    torchEnabled = enabled
                    camera?.cameraControl?.enableTorch(enabled)
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
