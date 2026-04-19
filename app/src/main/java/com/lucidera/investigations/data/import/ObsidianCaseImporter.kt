package com.lucidera.investigations.data.import

import com.lucidera.investigations.data.CaseDraft

object ObsidianCaseImporter {

    fun importCase(
        fileName: String,
        markdown: String
    ): CaseDraft {
        val frontmatter = parseFrontmatter(markdown)
        val title = extractLine(markdown, Regex("""# Investigation:\s*(.+)"""))
            ?: fileName.removeSuffix(".md").replace("_", " ")
        val caseCode = frontmatter["Case-ID"]
            ?: extractInlineValue(markdown, "Case ID")
            ?: fileName.substringBefore('_')
        val essentialQuestion = extractSection(markdown, "## Essential Question", "## Scope")
        val summary = extractSection(markdown, "## Working Summary", "## Lead Log")
        val primarySubject = extractInlineValue(markdown, "Primary subject")
        val publicationThreshold = extractInlineValue(markdown, "Publication threshold")
        val lead = extractInlineValue(markdown, "Lead")
        val classification = extractInlineValue(markdown, "Classification")
        val savePath = frontmatter["Save-Path"].orEmpty()
        val caseFolderName = savePath
            .replace('/', '\\')
            .trimEnd('\\')
            .substringAfterLast('\\', "${caseCode}_${title.replace(" ", "_")}")

        return CaseDraft(
            caseCode = caseCode.ifBlank { "CASE-IMPORT" },
            title = title.ifBlank { fileName.removeSuffix(".md") },
            essentialQuestion = essentialQuestion.ifBlank { "Imported from existing case note." },
            primarySubject = primarySubject.ifBlank { "Imported from Obsidian note" },
            classification = classification.ifBlank { "Imported case note" },
            leadInvestigator = lead.ifBlank { "Ethan Bradley" },
            summary = summary.ifBlank { "Imported from existing case note." },
            caseFolderName = caseFolderName,
            masterNoteName = if (fileName.endsWith(".md")) fileName else "$fileName.md",
            savePath = savePath,
            publicationThreshold = publicationThreshold.ifBlank {
                "Review imported note and confirm the publication threshold."
            }
        )
    }

    private fun parseFrontmatter(markdown: String): Map<String, String> {
        if (!markdown.startsWith("---")) return emptyMap()
        val end = markdown.indexOf("\n---", startIndex = 3)
        if (end == -1) return emptyMap()
        return markdown.substring(3, end)
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }
            .toMap()
    }

    private fun extractLine(markdown: String, regex: Regex): String? =
        regex.find(markdown)?.groupValues?.getOrNull(1)?.trim()

    private fun extractInlineValue(markdown: String, label: String): String =
        Regex("""\*\*$label:\*\*\s*(.+)""")
            .find(markdown)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.removeSuffix("  ")
            .orEmpty()

    private fun extractSection(markdown: String, startHeader: String, endHeader: String): String {
        val start = markdown.indexOf(startHeader)
        if (start == -1) return ""
        val afterStart = markdown.indexOf('\n', start).takeIf { it != -1 } ?: return ""
        val end = markdown.indexOf(endHeader, afterStart).takeIf { it != -1 } ?: markdown.length
        return markdown.substring(afterStart, end).trim()
    }
}
