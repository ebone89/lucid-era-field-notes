package com.lucidera.investigations

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityConfigChangeTest {

    @Test
    fun mainActivityHandlesRotationConfigChanges() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            PackageManager.ComponentInfoFlags.of(0)
        )

        assertTrue(activityInfo.configChanges and ActivityInfo.CONFIG_ORIENTATION != 0)
        assertTrue(activityInfo.configChanges and ActivityInfo.CONFIG_SCREEN_SIZE != 0)
    }
}
