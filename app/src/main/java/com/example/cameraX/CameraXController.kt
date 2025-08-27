package com.example.cameraX

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.Recorder
import androidx.camera.video.MediaStoreOutputOptions
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

object CameraXController {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var appContext: Context? = null
    private var mediaCapturedCallback: ((String) -> Unit)? = null
    private var recordingRef: Recording? = null

    fun setup(
        imgCap: ImageCapture?,
        vidCap: VideoCapture<Recorder>?,
        context: Context,
        onMediaCaptured: (String) -> Unit,
        setRecordingRef: (Recording?) -> Unit
    ) {
        imageCapture = imgCap
        videoCapture = vidCap
        appContext = context.applicationContext
        mediaCapturedCallback = onMediaCaptured
    }

    fun takePhoto() {
        val imgCap = imageCapture ?: run {
            Log.e("CameraX", "ImageCapture is not initialized yet")
            return
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Photos")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                appContext!!.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imgCap.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(appContext!!),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri.toString()
                    Log.d("CameraXController", "Photo saved: $uri")
                    mediaCapturedCallback?.invoke(uri) // ✅ use the callback you passed in setup()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraXController", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    fun startVideo() {
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            appContext!!.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).build()

        val recorder = videoCapture?.output
        var recording = recorder?.prepareRecording(appContext!!, mediaStoreOutput)

        // Only enable audio if permission is granted
        if (ContextCompat.checkSelfPermission(
                appContext!!,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            recording = recording?.withAudioEnabled()
        }

        recordingRef = recording?.start(
            ContextCompat.getMainExecutor(appContext!!)
        ) { event ->
            if (event is androidx.camera.video.VideoRecordEvent.Finalize) {
                val uri = event.outputResults.outputUri
                mediaCapturedCallback?.invoke(uri.toString())
            }
        }
    }

    fun stopVideo() {
        recordingRef?.stop()
        recordingRef = null
    }
}
