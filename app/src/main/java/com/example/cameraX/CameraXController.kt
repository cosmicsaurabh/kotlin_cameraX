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

object CameraXController {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var appContext: Context? = null

    private var mediaCapturedCallback: ((String) -> Unit)? = null
    private var recordingStateCallback: ((Boolean) -> Unit)? = null

    private var recordingRef: Recording? = null

    fun setup(
        imgCap: ImageCapture?,
        vidCap: VideoCapture<Recorder>?,
        context: Context,
        onMediaCaptured: (String) -> Unit,
        onRecordingStateChanged: (Boolean) -> Unit
    ) {
        imageCapture = imgCap
        videoCapture = vidCap
        appContext = context.applicationContext
        mediaCapturedCallback = onMediaCaptured
        recordingStateCallback = onRecordingStateChanged
    }

    fun takePhoto() {
        val imgCap = imageCapture ?: run {
            Log.e("CameraX", "ImageCapture not ready")
            return
        }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Photos")
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            appContext!!.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        imgCap.takePicture(
            output,
            ContextCompat.getMainExecutor(appContext!!),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    mediaCapturedCallback?.invoke(output.savedUri.toString())
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraXController", "Photo failed: ${exc.message}", exc)
                }
            }
        )
    }

    fun startVideo() {
        val ctx = appContext ?: return
        val vc = videoCapture ?: run {
            Log.e("CameraX", "VideoCapture not ready")
            return
        }
        val media = MediaStoreOutputOptions.Builder(
            ctx.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).build()

        var pending = vc.output.prepareRecording(ctx, media)
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            pending = pending.withAudioEnabled()
        }

        recordingRef = pending.start(ContextCompat.getMainExecutor(ctx)) { event ->
            when (event) {
                is androidx.camera.video.VideoRecordEvent.Start -> {
                    recordingStateCallback?.invoke(true)
                }
                is androidx.camera.video.VideoRecordEvent.Finalize -> {
                    mediaCapturedCallback?.invoke(event.outputResults.outputUri.toString())
                    recordingStateCallback?.invoke(false)
                }
                is androidx.camera.video.VideoRecordEvent.Status -> { /* optional */ }
            }
        }
    }

    fun stopVideo() {
        recordingRef?.stop()
        recordingRef = null
    }
}
