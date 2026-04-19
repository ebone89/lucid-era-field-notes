package com.lucidera.investigations.data.export

import android.content.Context
import androidx.core.net.toUri
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CasePackageExporter {

    fun buildPackageFileName(caseItem: InvestigationCaseEntity): String =
        "${caseItem.caseCode}_package.zip"

    fun writeCasePackage(
        context: Context,
        outputStream: OutputStream,
        markdownFileName: String,
        markdown: String,
        attachments: List<CaseAttachmentEntity>
    ) {
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry(markdownFileName))
            zip.write(markdown.toByteArray())
            zip.closeEntry()

            attachments.forEachIndexed { index, attachment ->
                val extension = attachment.fileName.substringAfterLast('.', "")
                val safeName = buildString {
                    append(index + 1)
                    append('_')
                    append(attachment.fileName.substringBeforeLast('.', attachment.fileName))
                    if (extension.isNotBlank()) {
                        append('.')
                        append(extension)
                    }
                }
                context.contentResolver.openInputStream(attachment.uri.toUri())?.use { input ->
                    zip.putNextEntry(ZipEntry("attachments/$safeName"))
                    input.copyTo(zip)
                    zip.closeEntry()
                }
            }
        }
    }
}
