package com.lucidera.investigations.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object MarkdownShareHelper {

    fun shareMarkdown(
        context: Context,
        fileName: String,
        markdown: String,
        chooserTitle: String
    ) {
        val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(shareDir, fileName)
        file.writeText(markdown)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
