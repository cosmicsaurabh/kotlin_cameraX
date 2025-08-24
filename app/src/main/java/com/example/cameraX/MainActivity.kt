import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import com.example.cameraX.CameraScreen
import android.widget.Toast

class MainActivity : ComponentActivity() {

    // List of required permissions (Camera + Audio for video recording)
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register permission launcher
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

        // Check if already granted
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
                CameraScreen()
            }
        }
    }
}
