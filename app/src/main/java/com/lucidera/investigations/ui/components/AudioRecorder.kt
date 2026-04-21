package com.lucidera.investigations.ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null

    fun startRecording(outputFile: File): Boolean {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        return try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            true
        } catch (e: IOException) {
            recorder.releaseSafely()
            Log.e("AudioRecorder", "prepare() failed", e)
            false
        } catch (e: RuntimeException) {
            recorder.releaseSafely()
            Log.e("AudioRecorder", "startRecording() failed", e)
            false
        } catch (e: SecurityException) {
            recorder.releaseSafely()
            Log.e("AudioRecorder", "startRecording() denied", e)
            false
        }
    }

    fun stopRecording(): Boolean {
        val recorder = mediaRecorder ?: return false
        return try {
            recorder.stop()
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stop() failed", e)
            false
        } finally {
            recorder.releaseSafely()
            mediaRecorder = null
        }
    }
}

private fun MediaRecorder.releaseSafely() {
    runCatching { release() }
}
