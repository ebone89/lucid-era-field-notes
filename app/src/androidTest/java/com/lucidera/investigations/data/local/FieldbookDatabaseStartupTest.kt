package com.lucidera.investigations.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FieldbookDatabaseStartupTest {

    @Test
    fun opensEncryptedDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = FieldbookDatabase.getDatabase(context)

        database.openHelper.writableDatabase.query("SELECT count(*) FROM sqlite_master").close()
    }
}
