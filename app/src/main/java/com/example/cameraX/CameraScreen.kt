package com.example.cameraX

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// NEW imports for Material icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn

@Composable
fun CameraScreen(vm: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    var targetRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }

    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                if (rotation != targetRotation) {
                    targetRotation = rotation
                    imageCapture?.targetRotation = rotation
                    videoCapture?.targetRotation = rotation
                }
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(vm.lensFacing) {
        scope.launch(Dispatchers.Main) {
            val provider = ProcessCameraProvider.getInstance(context).get()

            val preview = Preview.Builder()
                .setTargetRotation(targetRotation)
                .build()

            val imgCapture = ImageCapture.Builder()
                .setTargetRotation(targetRotation)
                .setFlashMode(vm.imageFlashMode)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            val vidCapture = VideoCapture.withOutput(recorder)

            try {
                provider.unbindAll()

                val selector = CameraSelector.Builder()
                    .requireLensFacing(vm.lensFacing)
                    .build()

                val cam = provider.bindToLifecycle(
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

                imgCapture.flashMode = vm.imageFlashMode
                cam.cameraControl.enableTorch(vm.isRecording && vm.flashEnabled)

                CameraXController.setup(
                    imgCapture,
                    vidCapture,
                    context,
                    onMediaCaptured = { uri -> Log.d("CameraApp", "Saved: $uri") },
                    onRecordingStateChanged = { active ->
                        vm.isRecording = active
                        camera?.cameraControl?.enableTorch(active && vm.flashEnabled)
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraApp", "Bind failed", e)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        // TOP-RIGHT: Flash toggle (Material icons)
        IconButton(
            onClick = {
                vm.flashEnabled = !vm.flashEnabled
                imageCapture?.flashMode = vm.imageFlashMode
                camera?.cameraControl?.enableTorch(vm.isRecording && vm.flashEnabled)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (vm.flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = if (vm.flashEnabled) "Flash On" else "Flash Off",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // BOTTOM BAR: left (switch), center (photo), right (video)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BOTTOM-LEFT: lens switch (Material icon)
            IconButton(
                onClick = {
                    vm.lensFacing =
                        if (vm.lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                },
                modifier = Modifier.padding(end = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // BOTTOM-CENTER: photo capture
            Button(
                onClick = { CameraXController.takePhoto() },
                enabled = !vm.isRecording,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) { Text("📸") }

            // BOTTOM-RIGHT: video start/stop
            Button(
                onClick = {
                    if (!vm.isRecording) {
                        camera?.cameraControl?.enableTorch(vm.flashEnabled)
                        CameraXController.startVideo()
                    } else {
                        CameraXController.stopVideo()
                        camera?.cameraControl?.enableTorch(false)
                    }
                },
                modifier = Modifier.padding(start = 24.dp)
            ) { Text(if (vm.isRecording) "⏹" else "🎥") }
        }
    }
}
