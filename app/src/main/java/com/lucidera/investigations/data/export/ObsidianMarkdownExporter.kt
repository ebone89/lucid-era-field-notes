package com.lucidera.investigations.data.export

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
        entities: List<EntityProfileEntity>
    ): String {
        val today = LocalDate.now().format(dateFormatter)
        val tags = buildList {
            add("investigation")
            add(caseItem.caseCode)
            add("osint")
        }.joinToString("\n") { "  - $it" }

        val leadRows = if (leads.isEmpty()) {
            "| | | | | |"
        } else {
            leads.joinToString("\n") { lead ->
                "| ${formatDate(lead.collectedAt)} | ${escapePipes(lead.sourceName)} | ${escapePipes(lead.summary)} | ${escapePipes(lead.archiveUrl)} | ${lead.status.name} |"
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
[Fill in on desktop]

**Out of bounds:**  
[Fill in on desktop]

**Primary subject:**  
${caseItem.primarySubject}

**Publication threshold:**  
${caseItem.publicationThreshold}

## Working Summary

${caseItem.summary}

## Lead Log
| Date | Source | Summary | Archive URL | Status |
|------|--------|---------|-------------|--------|
$leadRows

## Entity Map

$entityLinks

## Verified Findings

- [Add verified findings here]

## Open Threads

- [Add open threads here]

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
        entities: List<EntityProfileEntity>
    ): String {
        val today = LocalDate.now().format(dateFormatter)
        val openLeadLines = if (leads.isEmpty()) {
            "- No leads captured on mobile yet"
        } else {
            leads.joinToString("\n") { lead ->
                "- ${lead.sourceName}: ${lead.summary}"
            }
        }
        val entityLines = if (entities.isEmpty()) {
            "- No linked entities yet"
        } else {
            entities.joinToString("\n") { entity ->
                "- ${entity.name} (${entity.entityType.name.lowercase()})"
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
- Capture mobile notes without drifting away from the master case file

## Research Conducted
- Reviewed current case summary on mobile
- Added or updated leads and entities in the field app

## Leads Surfaced
$openLeadLines

## Findings
- Promote anything verified from this session into the master note on desktop

## Decisions Made
- Keep the mobile record aligned with the Obsidian case file
- Treat the app as a capture and export layer, not the canonical archive

## Entity Touchpoints
$entityLines

## Open Threads
- Review exported notes in Obsidian and merge anything that belongs in the master file
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

## Known Identifiers

| Identifier Type | Value | Source | Verified? |
|-----------------|-------|--------|-----------|
| Internal ID | ${entity.identifier} | Mobile field app | ${if (entity.confidence == com.lucidera.investigations.data.ConfidenceLevel.VERIFIED) "Yes" else "No"} |

## Associated Entities

- Linked case: [[10_Investigations/${caseItem.caseFolderName}/${caseItem.masterNoteName.removeSuffix(".md")}|${caseItem.caseCode}]]

## Appearances in Investigations

- [[10_Investigations/${caseItem.caseFolderName}/${caseItem.masterNoteName.removeSuffix(".md")}|${caseItem.title}]]

## Notes and Timeline

| Date | Event | Source | Archived? |
|------|-------|--------|-----------|
| $today | Entity note exported from mobile app | Lucid Era Field Notes | No |

## Source Appendix

| # | URL | Archive URL | Retrieved | Notes |
|---|-----|-------------|-----------|-------|
| 1 |  |  | $today | Fill in on desktop |

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
            .replace(Regex("\\s+"), "")
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

    private fun escapePipes(value: String): String = value.replace("|", "\\|")
}
