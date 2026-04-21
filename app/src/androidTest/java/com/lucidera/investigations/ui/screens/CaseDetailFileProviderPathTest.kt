package com.lucidera.investigations.ui.screens

import android.content.Context
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CaseDetailFileProviderPathTest {

    @Test
    fun exposesCameraCaptureFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val picturesDir = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val file = File.createTempFile("IMG_TEST_", ".jpg", picturesDir)

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            assertNotNull(uri)
        } finally {
            file.delete()
        }
    }

    @Test
    fun exposesRecordedAudioFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recordingsDir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val file = File.createTempFile("REC_TEST_", ".mp4", recordingsDir)

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            assertNotNull(uri)
        } finally {
            file.delete()
        }
    }
}
