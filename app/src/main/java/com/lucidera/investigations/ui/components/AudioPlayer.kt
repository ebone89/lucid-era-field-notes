package com.lucidera.investigations.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playUri(uri: Uri) {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "playUri failed", e)
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
}
