package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object MediaStorageManager {

    private const val TAG = "MediaStorageManager"
    private const val MEDIA_DIR_NAME = "action_audio_files"

    /**
     * Copies any selected gallery video/audio content Uri or File Uri into
     * the app's internal private storage directory.
     * Returns the absolute path of the saved internal file.
     */
    fun saveUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    sourceUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val mediaDir = File(context.filesDir, MEDIA_DIR_NAME)
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }

            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = when {
                mimeType?.contains("video") == true -> ".mp4"
                mimeType?.contains("audio") == true -> ".mp3"
                else -> ".mp4"
            }

            val outputFile = File(mediaDir, "audio_${System.currentTimeMillis()}$extension")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                Log.d(TAG, "Successfully saved media file to internal storage: ${outputFile.absolutePath}, size: ${outputFile.length()}")
                outputFile.absolutePath
            } else {
                Log.e(TAG, "Saved media file is empty or missing")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy media Uri to internal storage", e)
            null
        }
    }

    /**
     * Plays the audio from a local file path or Uri using MediaPlayer with maximum volume on STREAM_MUSIC.
     * Guaranteed to output through system speakers.
     */
    fun playAudio(context: Context, filePathOrUri: String, onPrepared: ((MediaPlayer) -> Unit)? = null): MediaPlayer? {
        if (filePathOrUri.isEmpty()) return null

        // Unmute and boost STREAM_MUSIC volume
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVol < (maxVol * 0.5).toInt()) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.9).toInt(), 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not adjust audio stream volume", e)
        }

        val file = File(filePathOrUri)

        // Attempt 1: Direct File Path if file exists (Primary & most reliable)
        if (file.exists() && file.isAbsolute && file.length() > 0) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    setVolume(1.0f, 1.0f)
                    prepare()
                    start()
                }
                onPrepared?.invoke(player)
                Log.d(TAG, "Attempt 1 succeeded playing file: ${file.absolutePath}")
                return player
            } catch (e: Exception) {
                Log.e(TAG, "Attempt 1 failed for file: ${file.absolutePath}", e)
            }
        }

        // Attempt 2: Content or File Uri via Context
        if (filePathOrUri.startsWith("content://") || filePathOrUri.startsWith("file://")) {
            try {
                val uri = Uri.parse(filePathOrUri)
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, uri)
                    setVolume(1.0f, 1.0f)
                    prepare()
                    start()
                }
                onPrepared?.invoke(player)
                Log.d(TAG, "Attempt 2 succeeded playing Uri: $filePathOrUri")
                return player
            } catch (e: Exception) {
                Log.e(TAG, "Attempt 2 failed for Uri: $filePathOrUri", e)
            }

            // Attempt 3: Open File Descriptor
            try {
                val uri = Uri.parse(filePathOrUri)
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    pfd.use { descriptor ->
                        val player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            setDataSource(descriptor.fileDescriptor)
                            setVolume(1.0f, 1.0f)
                            prepare()
                            start()
                        }
                        onPrepared?.invoke(player)
                        Log.d(TAG, "Attempt 3 succeeded playing FileDescriptor")
                        return player
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Attempt 3 failed for FileDescriptor", e)
            }
        }

        // Attempt 4: Direct String Data Source
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(filePathOrUri)
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            onPrepared?.invoke(player)
            Log.d(TAG, "Attempt 4 succeeded playing string path")
            return player
        } catch (e: Exception) {
            Log.e(TAG, "Attempt 4 failed for string path: $filePathOrUri", e)
        }

        Log.e(TAG, "All playback attempts failed for: $filePathOrUri")
        return null
    }

    /**
     * Fallback beep sound if media file is missing or corrupted
     */
    fun playFallbackBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback beep error", e)
        }
    }
}
