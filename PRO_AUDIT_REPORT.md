# PRO AUDIT REPORT: Lucid Era Field Notes

**Date:** 2026-04-20
**Auditor:** Claude Code (Anthropic) on behalf of Ethan Bradley
**Branch:** `feature/mobile-capture-polish`
**DB Version:** 6
**Build:** assembleDebug SUCCESSFUL

---

## 1. Scope

Full audit of the Lucid Era Field Notes Android codebase across two passes (2026-04-19 and 2026-04-20). First pass fixed bugs in the base architecture. Second pass reviewed Codex-generated feature additions, caught one regression, and verified the build.

---

## 2. Architecture Assessment

Standard MVVM with clean three-layer separation.

| Layer | Implementation | Status |
|---|---|---|
| UI | Jetpack Compose screens + components | Sound |
| State | ViewModel + StateFlow + combine | Sound |
| Data | Room DAOs + Repository + Retrofit | Sound |

Navigation Compose handles the route graph. `AppContainer` wires dependencies manually. Acceptable at this scale. `InvestigationRepository` is a clean pass-through with no business logic in the UI layer. All five ViewModels expose a single `uiState: StateFlow<XxxUiState>` with `SharingStarted.WhileSubscribed(5_000)`.

---

## 3. Feature Completion Tracker

### 3.1 Done (both passes)

| Feature | Files Changed | Status |
|---|---|---|
| KSP migration (from kapt) | build.gradle.kts | Done before first pass |
| Active cases count fix | DashboardViewModel | Fixed pass 1 |
| Entity filename whitespace fix | ObsidianMarkdownExporter | Fixed pass 1 |
| Release build HTTP logging | AppContainer | Fixed pass 1 |
| Safe enum deserialization | Converters | Fixed pass 1 |
| ViewModel error surface | CaseDetailViewModel, CaseDetailScreen | Done pass 2 (Codex + review) |
| EXIF/GPS metadata extraction | CaseDetailScreen, CaseAttachmentEntity | Done pass 2 |
| MIME type on attachments | CaseAttachmentEntity, AttachmentDraft | Done pass 2 |
| Entity aliases | EntityProfileEntity, EntityDraft, dialogs | Done pass 2 |
| Lead category tags | LeadEntity, LeadDraft, dialogs | Done pass 2 |
| Lead and entity delete | DAOs, Repository, ViewModel | Done pass 2 |
| Lead and entity edit | DAOs, Repository, ViewModel | Done pass 2 |
| Attachment caption edit and delete | AttachmentDao, Repository, ViewModel | Done pass 2 |
| Explicit Room migrations (2-3, 3-4, 4-5, 5-6) | FieldbookDatabase | Done pass 2 |
| Room schema export with KSP arg | FieldbookDatabase, build.gradle.kts | Done (regression caught pass 2) |
| Schema snapshots committed (v4, v5, v6) | app/schemas/ | Done pass 2 |

### 3.2 Regression Fixed in Pass 2

**`exportSchema = false` (FieldbookDatabase.kt):** Codex reverted `exportSchema` from `true` to `false` during the feature additions. The KSP `room.schemaLocation` argument was still in `build.gradle.kts`, but with `exportSchema = false`, KSP does not write the schema file. The v6 schema did not exist. Fixed to `true`. v6.json generated and committed.

### 3.3 Minor Fix in Pass 2

**Known Aliases markdown format (ObsidianMarkdownExporter.kt):** The `## Known Aliases` section in entity notes was outputting the raw comma-separated string instead of a bullet list. Changed to `split(",").joinToString("\n") { "- ${it.trim()}" }` with a `- None recorded` default when blank.

### 3.4 Outstanding Items

| Item | Priority | Notes |
|---|---|---|
| P4: Bulk export (all cases) | Medium | No schema change needed. Extend CasePackageExporter. Wire in Dashboard overflow. |
| P7: Encrypted storage | Low | SQLCipher. Significant implementation risk. Manual review required before implementation. |
| tags/aliases fields lack default values in drafts | Low | `LeadDraft.tags` and `EntityDraft.aliases` are not nullable and have no defaults. If a new call site creates a draft without these fields, it will fail to compile. Easy to catch but worth noting for future callers. |
| ArchiveViewModel uses `tags = "archive"` hardcoded | Low | Reasonable default for Wayback-sourced leads. No action needed unless the investigator wants to customize. |

---

## 4. Database Migration Chain

All migrations are explicit. No `fallbackToDestructiveMigration`.

| Migration | What Changed |
|---|---|
| 2 to 3 | Created `case_attachments` table |
| 3 to 4 | Added `gpsLat`, `gpsLon`, `capturedAt`, `deviceModel` to `case_attachments` |
| 4 to 5 | Added `mimeType` to `case_attachments` |
| 5 to 6 | Added `tags` to `leads`, `aliases` to `entity_profiles` |

Schema snapshots for versions 4, 5, and 6 are committed under `app/schemas/`.

---

## 5. Obsidian Export Status

Three export types: case master note, session log, entity note.

| Feature | Status |
|---|---|
| YAML frontmatter with Case-ID, Status, Tags, Save-Path | Working |
| Lead log table with Tags column | Working |
| Entity wiki links with corrected underscored filenames | Working |
| Known Aliases section as bullet list | Fixed pass 2 |
| Attachment MIME type in export | Working |
| GPS coordinates in attachment block | Working |
| EXIF timestamp in attachment block | Working |
| Session log attachment block with GPS and timestamp | Working |
| Source appendix | Working |

---

## 6. Vulnerability Surface

| Item | Risk | Status |
|---|---|---|
| HTTP logging in release builds | Medium | Fixed pass 1 (NONE in release) |
| Wayback API no timeout | Medium | Fixed pass 1 (15s connect + read) |
| Enum deserialization crash on unknown value | Medium | Fixed pass 1 (runCatching with defaults) |
| `fallbackToDestructiveMigration` | Critical | Removed in hardening pass |
| Local data unencrypted at rest | Medium | Android FBE provides baseline. SQLCipher path documented in Section 3.4. |
| Wayback lookups visible to archive.org | Informational | Inherent to the API. No mitigation available client-side. |

---

## 7. Build System

| Item | Status |
|---|---|
| KSP (no kapt) | Done |
| Room schema export | Done (exportSchema = true, KSP arg set) |
| Schema snapshots in source control | Done (v4, v5, v6) |
| BuildConfig enabled | Done |
| ExifInterface dependency | Added |
| Minification disabled | Acceptable for single-operator tool. Enable before broader distribution. |
| Compose BOM pinned at 2024.10.01 | Not the latest. Update before next release cycle. |

---

## 8. Final Assessment

The codebase is in production-grade shape for a single-operator field tool. Every major risk from the first audit pass has been resolved. The migration chain is complete and explicit. Export output maps correctly to the vault structure. EXIF capture, edit/delete controls, and user feedback are all working.

The one meaningful capability gap remaining is bulk export (P4). Everything else on the outstanding list is low-priority or requires a deliberate architecture decision (encrypted storage).

---

*Second pass completed 2026-04-20.*
