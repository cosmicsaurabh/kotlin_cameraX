package com.example.cameraX

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
 * - Flash toggle
 */
@Composable
fun ControlsBar(
    onTakePhoto: () -> Unit,
    onToggleVideo: (Boolean) -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleFlash: (Boolean) -> Unit
) {
    var isFlashOn by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

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

        // 📸 Photo button (only visible when not recording video)
        if (!isRecording) {
            OutlinedButton(onClick = onTakePhoto) {
                Text("📸 Photo")
            }
        }

        // 🎥 Video button (toggles start/stop)
        OutlinedButton(
            onClick = {
                isRecording = !isRecording
                onToggleVideo(isRecording)
            }
        ) {
            Text(if (isRecording) "⏹ Stop" else "🎥 Video")
        }

        // ⚡ Flash toggle button
        IconButton(onClick = {
            isFlashOn = !isFlashOn
            onToggleFlash(isFlashOn)
        }) {
            Icon(
                imageVector = if (isFlashOn)
                    ImageVector.vectorResource(id = R.drawable.ic_flash_on)
                else
                    ImageVector.vectorResource(id = R.drawable.ic_flash_off),
                contentDescription = if (isFlashOn) "Flash On" else "Flash Off",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
