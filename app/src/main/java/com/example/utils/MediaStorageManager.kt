package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
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
                Log.d(TAG, "Successfully saved media file to internal storage: ${outputFile.absolutePath}")
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

        // Unmute and boost STREAM_MUSIC volume if low
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVol < (maxVol * 0.4).toInt()) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.85).toInt(), 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not adjust audio stream volume", e)
        }

        var player: MediaPlayer? = null
        var fileInputStream: FileInputStream? = null

        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                val file = File(filePathOrUri)
                if (file.exists() && file.isAbsolute) {
                    fileInputStream = FileInputStream(file)
                    setDataSource(fileInputStream!!.fd)
                } else if (filePathOrUri.startsWith("content://") || filePathOrUri.startsWith("file://")) {
                    val uri = Uri.parse(filePathOrUri)
                    setDataSource(context, uri)
                } else {
                    setDataSource(filePathOrUri)
                }

                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            onPrepared?.invoke(player)
            return player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file directly: $filePathOrUri", e)
            player?.release()
            try { fileInputStream?.close() } catch (_: Exception) {}

            // Fallback beep tone so user ALWAYS gets loud speaker feedback
            playFallbackBeep()
            return null
        }
    }

    /**
     * Fallback beep sound if media codec fails or file is corrupted
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
