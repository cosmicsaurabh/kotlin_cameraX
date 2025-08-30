package com.example.cameraX

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

/**
 * Bottom control bar with buttons:
 * - Switch camera (front/back)
 * - Take photo
 * - Start/stop video recording
 * - Flash mode (for stills) toggle
 * - Torch (continuous light) toggle
 *
 * IMPORTANT: This composable is "dumb UI".
 * It does NOT own any camera-related booleans. It receives them as parameters.
 * This avoids desync between icons and actual camera state.
 */
@Composable
fun ControlsBar(
    isRecording: Boolean,
    flashMode: Int, // Now takes the ImageCapture flash mode
    isTorchOn: Boolean, // Separate state for continuous torch
    onTakePhoto: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleFlash: (Int) -> Unit, // Callback takes the new flash mode
    onToggleTorch: (Boolean) -> Unit // Callback for continuous torch
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🔄 Switch camera button
        IconButton(onClick = onSwitchCamera) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_switch),
                contentDescription = "Switch Camera",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 📸 Photo button (disabled while recording video)
        OutlinedButton(
            onClick = onTakePhoto,
            enabled = !isRecording
        ) {
            Text("📸 Photo")
        }

        // 🎥 Video button (start/stop)
        OutlinedButton(onClick = onToggleVideo) {
            Text(if (isRecording) "⏹ Stop" else "🎥 Video")
        }

        // ⚡ Flash (for photo stills) toggle - now cycles through OFF, ON, AUTO
        IconButton(
            onClick = {
                val newFlashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
                    else -> ImageCapture.FLASH_MODE_OFF // Default
                }
                onToggleFlash(newFlashMode)
            },
            enabled = !isRecording // Flash mode for stills is typically disabled during video
        ) {
            val iconRes = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> R.drawable.ic_flash_off
                ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
                ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto // You'll need this drawable
                else -> R.drawable.ic_flash_off
            }
            val contentDesc = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> "Flash Off"
                ImageCapture.FLASH_MODE_ON -> "Flash On"
                ImageCapture.FLASH_MODE_AUTO -> "Flash Auto"
                else -> "Flash Off"
            }
            Icon(
                imageVector = ImageVector.vectorResource(id = iconRes),
                contentDescription = contentDesc,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 💡 Torch (continuous light for video/dark scene) toggle
        IconButton(
            onClick = { onToggleTorch(!isTorchOn) },
            // Torch can be independent, but often used with video or in very dark scenes.
            // You might enable/disable based on recording if desired.
        ) {
            Icon(
                imageVector = if (isTorchOn)
                    ImageVector.vectorResource(id = R.drawable.ic_torch_on) // You'll need this drawable
                else
                    ImageVector.vectorResource(id = R.drawable.ic_torch_off), // You'll need this drawable
                contentDescription = if (isTorchOn) "Torch On" else "Torch Off",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}