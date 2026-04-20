package com.lucidera.investigations.data.export

import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.local.entity.LeadEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ObsidianMarkdownExporter {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun buildCaseMarkdown(
        caseItem: InvestigationCaseEntity,
        leads: List<LeadEntity>,
        entities: List<EntityProfileEntity>,
        attachments: List<CaseAttachmentEntity> = emptyList()
    ): String {
        val today = LocalDate.now().format(dateFormatter)
        val tags = buildList {
            add("investigation")
            add(caseItem.caseCode)
            add("osint")
        }.joinToString("\n") { "  - $it" }

        val leadRows = if (leads.isEmpty()) {
            "| | | | | | |"
        } else {
            leads.joinToString("\n") { lead ->
                "| ${formatDate(lead.collectedAt)} | ${escapePipes(lead.sourceName)} | ${escapePipes(lead.summary)} | ${escapePipes(lead.tags)} | ${escapePipes(lead.archiveUrl)} | ${lead.status.name} |"
            }
        }

        val entityLinks = if (entities.isEmpty()) {
            "- None linked yet"
        } else {
            entities.joinToString("\n") { entity ->
                "- [[20_Entities/${buildEntityFileName(entity)}|${entity.name}]]"
            }
        }

        val sourceAppendix = if (leads.isEmpty()) {
            "| 1 |  |  |  |  |"
        } else {
            leads.mapIndexed { index, lead ->
                "| ${index + 1} | ${escapePipes(lead.sourceUrl)} | ${escapePipes(lead.archiveUrl)} | ${formatDate(lead.collectedAt)} | ${escapePipes(lead.summary)} |"
            }.joinToString("\n")
        }

        val attachmentLines = if (attachments.isEmpty()) {
            "- No attachments logged in the app for this case."
        } else {
            attachments.joinToString("\n") { attachment ->
                buildString {
                    append("- ${attachment.fileName} (${attachment.attachmentType.name.lowercase()})${if (attachment.caption.isNotBlank()) ": ${attachment.caption}" else ""}  \n  Type: `${attachment.mimeType}`  \n  Local URI: `${attachment.uri}`")
                    if (attachment.gpsLat != null && attachment.gpsLon != null) {
                        append("  \n  GPS: ${"%.6f".format(attachment.gpsLat)}, ${"%.6f".format(attachment.gpsLon)}")
                    }
                    if (attachment.capturedAt != null) {
                        append("  \n  Captured: ${formatDateTime(attachment.capturedAt)}")
                    }
                }
            }
        }

        return """
---
Status: ${caseItem.status.name.lowercase().replaceFirstChar(Char::uppercase)}
Case-ID: ${caseItem.caseCode}
Created: ${formatDate(caseItem.createdAt)}
Last-Updated: $today
Tags:
$tags
Save-Path: ${caseItem.savePath}
---

# Investigation: ${caseItem.title}

**Case ID:** ${caseItem.caseCode}  
**Lead:** ${caseItem.leadInvestigator}  
**Classification:** ${caseItem.classification}  

## Essential Question

${caseItem.essentialQuestion}

## Scope
**In bounds:**  
[Add after source review]

**Out of bounds:**  
[Add after source review]

**Primary subject:**  
${caseItem.primarySubject}

**Publication threshold:**  
${caseItem.publicationThreshold}

## Working Summary

${caseItem.summary}

## Lead Log
| Date | Source | Summary | Tags | Archive URL | Status |
|------|--------|---------|------|-------------|--------|
$leadRows

## Entity Map

$entityLinks

## Verified Findings

- [Add verified findings here]

## Open Threads

- [Add open threads here]

## Attachments

$attachmentLines

## Source Appendix
| # | URL | Archive URL | Retrieved | Notes |
|---|-----|-------------|-----------|-------|
$sourceAppendix

## Related Notes

- [[10_Investigations/Case_Registry]]
- [[Filesystem_Context]]
- [[00_Framework/Core_Workflow]]
${buildEntityRelatedNotes(entities)}
""".trimIndent()
    }

    fun buildSessionLogMarkdown(
        caseItem: InvestigationCaseEntity,
        leads: List<LeadEntity>,
        entities: List<EntityProfileEntity>,
        attachments: List<CaseAttachmentEntity> = emptyList()
    ): String {
        val today = LocalDate.now().format(dateFormatter)
        val openLeadLines = if (leads.isEmpty()) {
            "- No leads logged in this session."
        } else {
            leads.joinToString("\n") { lead ->
                "- ${lead.sourceName}: ${lead.summary}"
            }
        }
        val entityLines = if (entities.isEmpty()) {
            "- No linked entities logged in this session."
        } else {
            entities.joinToString("\n") { entity ->
                "- ${entity.name} (${entity.entityType.name.lowercase()})"
            }
        }

        val attachmentLines = if (attachments.isEmpty()) {
            "- No photos or attachments logged in this session."
        } else {
            attachments.joinToString("\n") { attachment ->
                buildString {
                    append("- ${attachment.fileName}: ${attachment.caption.ifBlank { "Caption still needed." }}  \n  Type: `${attachment.mimeType}`")
                    if (attachment.gpsLat != null && attachment.gpsLon != null) {
                        append("  \n  GPS: ${"%.6f".format(attachment.gpsLat)}, ${"%.6f".format(attachment.gpsLon)}")
                    }
                    if (attachment.capturedAt != null) {
                        append("  \n  Captured: ${formatDateTime(attachment.capturedAt)}")
                    }
                }
            }
        }

        return """
---
Status: Closed
Case-ID: ${caseItem.caseCode}
Type: Session Log
Created: $today
Tags:
  - session-log
  - ${caseItem.caseCode}
Save-Path: ${caseItem.savePath}
---

# Session Log: ${caseItem.caseCode} -- $today

## Session Goals
- Record field notes that can be reviewed and folded back into the case file

## Research Conducted
- Reviewed the active case summary
- Logged leads, entities, or attachments in the field

## Leads Surfaced
$openLeadLines

## Findings
- Move anything verified from this session into the case note after review

## Decisions Made
- Keep the phone record aligned with the Obsidian case file
- Treat the app as a field log, not the final archive

## Entity Touchpoints
$entityLines

## Attachment Capture
$attachmentLines

## Open Threads
- Review exported notes and merge anything that belongs in the master file
""".trimIndent()
    }

    fun buildEntityMarkdown(
        entity: EntityProfileEntity,
        caseItem: InvestigationCaseEntity
    ): String {
        val today = LocalDate.now().format(dateFormatter)
        val entityFileName = buildEntityFileName(entity)
        return """
---
Status: Active
Entity-Type: ${entity.entityType.name.lowercase().replaceFirstChar(Char::uppercase)}
Confidence: ${entity.confidence.name.lowercase().replaceFirstChar(Char::uppercase)}
Created: $today
Last-Updated: $today
Tags:
  - entity
  - osint
Save-Path: A:\Obsidian_Vaults\Main-Notes\03_Organizations\03_Lucid_Era_Group\031-Lucid_Era_Investigations\20_Entities
---

# ENTITY: ${entity.name}

**Type:** ${entity.entityType.name.lowercase().replaceFirstChar(Char::uppercase)}  
**Status:** Active  
**Confidence:** ${entity.confidence.name.lowercase().replaceFirstChar(Char::uppercase)}  

## Summary
${entity.summary}

## Known Aliases
${if (entity.aliases.isBlank()) "- None recorded" else entity.aliases.split(",").joinToString("\n") { "- ${it.trim()}" }}

## Known Identifiers

| Identifier Type | Value | Source | Verified? |
|-----------------|-------|--------|-----------|
| Internal ID | ${entity.identifier} | Field capture app | ${if (entity.confidence == com.lucidera.investigations.data.ConfidenceLevel.VERIFIED) "Yes" else "No"} |

## Associated Entities

- Linked case: [[10_Investigations/${caseItem.caseFolderName}/${caseItem.masterNoteName.removeSuffix(".md")}|${caseItem.caseCode}]]

## Appearances in Investigations

- [[10_Investigations/${caseItem.caseFolderName}/${caseItem.masterNoteName.removeSuffix(".md")}|${caseItem.title}]]

## Notes and Timeline

| Date | Event | Source | Archived? |
|------|-------|--------|-----------|
| $today | Entity note exported from the field app | Lucid Era Field Notes | No |

## Source Appendix

| # | URL | Archive URL | Retrieved | Notes |
|---|-----|-------------|-----------|-------|
| 1 |  |  | $today | Add source details during desktop review |

## Related Notes

- [[10_Investigations/${caseItem.caseFolderName}/${caseItem.masterNoteName.removeSuffix(".md")}]]
- [[20_Entities/$entityFileName|${entity.name}]]
- [[Filesystem_Context]]
""".trimIndent()
    }

    fun buildEntityFileName(entity: EntityProfileEntity): String {
        val safeName = entity.name
            .replace("/", "-")
            .replace("\\", "-")
            .replace(".", "-")
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^A-Za-z0-9_-]"), "")
        val typeName = entity.entityType.name.lowercase().replaceFirstChar(Char::uppercase)
        return "ENT_${safeName}_$typeName"
    }

    fun buildSessionLogFileName(caseItem: InvestigationCaseEntity): String {
        val today = LocalDate.now().format(dateFormatter)
        return "${caseItem.caseCode}_SessionLog_$today.md"
    }
    private fun buildEntityRelatedNotes(entities: List<EntityProfileEntity>): String =
        entities.take(3).joinToString("\n") { entity ->
            "- [[20_Entities/${buildEntityFileName(entity)}|${entity.name}]]"
        }

    private fun formatDate(timestamp: Long): String =
        dateFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate())

    private fun formatDateTime(timestamp: Long): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

    private fun escapePipes(value: String): String = value.replace("|", "\\|")
}
