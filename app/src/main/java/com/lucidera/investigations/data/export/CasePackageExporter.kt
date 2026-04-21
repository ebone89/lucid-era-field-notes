package com.lucidera.investigations.data.export

import android.content.Context
import androidx.core.net.toUri
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.local.entity.LeadEntity
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CasePackageExporter {

    fun buildPackageFileName(caseItem: InvestigationCaseEntity): String =
        "${caseItem.caseCode}_package.zip"

    fun buildVaultPackageFileName(): String {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return "vault_export_$date.zip"
    }

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
                writeAttachmentEntry(context, zip, attachment, index, prefix = "attachments/")
            }
        }
    }

    suspend fun writeVaultPackage(
        context: Context,
        outputStream: OutputStream,
        cases: List<InvestigationCaseEntity>,
        leadsFor: suspend (Long) -> List<LeadEntity>,
        entitiesFor: suspend (Long) -> List<EntityProfileEntity>,
        attachmentsFor: suspend (Long) -> List<CaseAttachmentEntity>
    ) {
        ZipOutputStream(outputStream).use { zip ->
            cases.forEach { caseItem ->
                val folder = caseItem.caseFolderName
                val leads = leadsFor(caseItem.id)
                val entities = entitiesFor(caseItem.id)
                val attachments = attachmentsFor(caseItem.id)
                val markdown = ObsidianMarkdownExporter.buildCaseMarkdown(caseItem, leads, entities, attachments)

                zip.putNextEntry(ZipEntry("$folder/${caseItem.masterNoteName}"))
                zip.write(markdown.toByteArray())
                zip.closeEntry()

                attachments.forEachIndexed { index, attachment ->
                    writeAttachmentEntry(context, zip, attachment, index, prefix = "$folder/attachments/")
                }
            }
        }
    }

    private fun writeAttachmentEntry(
        context: Context,
        zip: ZipOutputStream,
        attachment: CaseAttachmentEntity,
        index: Int,
        prefix: String
    ) {
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
            zip.putNextEntry(ZipEntry("$prefix$safeName"))
            input.copyTo(zip)
            zip.closeEntry()
        }
    }
}
