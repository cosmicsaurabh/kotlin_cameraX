package com.example.cameraX

import androidx.camera.core.ImageCapture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.camera.core.CameraSelector

class CameraViewModel : ViewModel() {

    // current lens (survives rotation)
    var lensFacing by mutableIntStateOf(CameraSelector.LENS_FACING_BACK)

    // is recording now
    var isRecording by mutableStateOf(false)

    // single flash toggle that drives BOTH:
    // - ImageCapture.flashMode (ON/OFF)
    // - Torch during video (ON while recording if enabled)
    var flashEnabled by mutableStateOf(false)

    // helper for ImageCapture
    val imageFlashMode: Int
        get() = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
}
