package com.lucidera.investigations.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AudioRecorderTest {

    @Test
    fun recordsSuccessfullyWhenPermissionIsGranted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = createRecordingFile(context)
        runShell("pm grant ${context.packageName} ${Manifest.permission.RECORD_AUDIO}")
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        )

        val recorder = AudioRecorder(context)
        val started = recorder.startRecording(file)
        try {
            assertTrue(started)
            Thread.sleep(1_000)
            assertTrue(recorder.stopRecording())
        } finally {
            file.delete()
        }
    }

    private fun createRecordingFile(context: Context): File {
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        return File.createTempFile("REC_TEST_", ".mp4", dir)
    }

    private fun runShell(command: String) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand(command).close()
        SystemClock.sleep(300)
    }
}
