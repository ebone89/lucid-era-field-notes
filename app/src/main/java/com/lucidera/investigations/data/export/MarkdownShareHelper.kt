package com.lucidera.investigations.data.export

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
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

    fun shareCasePackage(
        context: Context,
        fileName: String,
        markdown: String,
        attachments: List<CaseAttachmentEntity>,
        chooserTitle: String
    ) {
        val markdownUri = writeSharedMarkdown(context, fileName, markdown)
        val uris = arrayListOf<Uri>(markdownUri).apply {
            addAll(attachments.map { it.uri.toUri() })
        }

        val clipData = ClipData.newUri(context.contentResolver, fileName, uris.first()).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun writeSharedMarkdown(
        context: Context,
        fileName: String,
        markdown: String
    ): Uri {
        val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(shareDir, fileName)
        file.writeText(markdown)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
