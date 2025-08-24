package com.example.cameraX

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.example.cameraX.R

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
        // Switch camera (front/back)
        IconButton(onClick = onSwitchCamera) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_switch),
                contentDescription = "Switch Camera",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Take photo (only if not recording video)
        if (!isRecording) {
            OutlinedButton(onClick = onTakePhoto) {
                Text("📸 Photo")
            }
        }

        // Record video (toggle start/stop)
        OutlinedButton(
            onClick = {
                isRecording = !isRecording
                onToggleVideo(isRecording)
            }
        ) {
            Text(if (isRecording) "⏹ Stop" else "🎥 Video")
        }

        // Flash toggle
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
