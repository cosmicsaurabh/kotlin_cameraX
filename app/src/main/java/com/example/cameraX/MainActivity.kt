package com.example.cameraX // Or your actual package name

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import android.widget.Toast

/**
 * Requests camera & audio permissions and shows CameraScreen.
 */
class MainActivity : ComponentActivity() {

    // Required permissions (Audio is needed only for video recording with sound)
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permission launcher
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.values.all { it }
            if (granted) {
                showCameraScreen()
            } else {
                Toast.makeText(this, "Camera & audio permissions are required", Toast.LENGTH_SHORT).show()
            }
        }

        // Ask if not granted
        if (allPermissionsGranted()) {
            showCameraScreen()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    /** Checks if all permissions are granted */
    private fun allPermissionsGranted(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Sets the Compose content */
    private fun showCameraScreen() {
        setContent {
            MaterialTheme {
                CameraScreen() // ViewModel provided inside via viewModel()
            }
        }
    }
}
