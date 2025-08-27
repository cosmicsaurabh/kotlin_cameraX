package com.example.cameraX

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Singleton controller to handle CameraX actions like
 * taking photos and recording videos.
 */
object CameraXController {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var appContext: Context? = null
    private var mediaCapturedCallback: ((String) -> Unit)? = null
    private var recordingRef: Recording? = null

    /**
     * Initialize controller with ImageCapture & VideoCapture use cases
     */
    fun setup(
        imgCap: ImageCapture?,
        vidCap: VideoCapture<Recorder>?,
        context: Context,
        onMediaCaptured: (String) -> Unit
    ) {
        imageCapture = imgCap
        videoCapture = vidCap
        appContext = context.applicationContext
        mediaCapturedCallback = onMediaCaptured
    }

    /**
     * Capture a photo and save it into MediaStore (Gallery)
     */
    fun takePhoto() {
        val imgCap = imageCapture ?: run {
            Log.e("CameraX", "ImageCapture is not initialized yet")
            return
        }

        // Generate file name using timestamp
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())

        // Define MediaStore metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Photos")
        }

        // Output options to MediaStore
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            appContext!!.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // Take picture
        imgCap.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(appContext!!),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri.toString()
                    Log.d("CameraXController", "Photo saved: $uri")
                    mediaCapturedCallback?.invoke(uri)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraXController", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    /**
     * Start recording a video and save it into MediaStore
     */
    fun startVideo() {
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            appContext!!.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).build()

        val recorder = videoCapture?.output
        var recording = recorder?.prepareRecording(appContext!!, mediaStoreOutput)

        // Enable audio only if RECORD_AUDIO permission is granted
        if (ContextCompat.checkSelfPermission(
                appContext!!,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            recording = recording?.withAudioEnabled()
        }

        // Start recording
        recordingRef = recording?.start(
            ContextCompat.getMainExecutor(appContext!!)
        ) { event ->
            if (event is androidx.camera.video.VideoRecordEvent.Finalize) {
                val uri = event.outputResults.outputUri
                mediaCapturedCallback?.invoke(uri.toString())
            }
        }
    }

    /**
     * Stop video recording
     */
    fun stopVideo() {
        recordingRef?.stop()
        recordingRef = null
    }
}
