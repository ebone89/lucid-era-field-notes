# PRO AUDIT REPORT: Lucid Era Field Notes

**Date:** 2026-04-19 (Pass 3)
**Auditor:** Claude Code (Anthropic) on behalf of Ethan Bradley
**Branch:** `feature/mobile-capture-polish`
**DB Version:** 6
**Build:** assembleDebug SUCCESSFUL (Pass 3)

---

## 1. Scope

Full audit of the Lucid Era Field Notes Android codebase across three passes.

- **Pass 1 (2026-04-19):** Baseline audit; fixed architecture bugs in the original scaffolded code.
- **Pass 2 (2026-04-20):** Reviewed all Codex feature additions. Caught one regression (`exportSchema` reverted to false). Fixed it. Committed v6 schema snapshot.
- **Pass 3 (2026-04-19):** Implemented bulk vault export (P4). Full re-audit of every source file. No regressions introduced. Two new low-priority items added to outstanding list.

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

### 3.1 Done (all passes)

| Feature | Files Changed | Status |
|---|---|---|
| KSP migration (from kapt) | build.gradle.kts | Done before Pass 1 |
| Active cases count fix | DashboardViewModel | Fixed Pass 1 |
| Entity filename whitespace fix | ObsidianMarkdownExporter | Fixed Pass 1 |
| Release build HTTP logging | AppContainer | Fixed Pass 1 |
| Safe enum deserialization | Converters | Fixed Pass 1 |
| ViewModel error surface | CaseDetailViewModel, CaseDetailScreen | Done Pass 2 (Codex + review) |
| EXIF/GPS metadata extraction | CaseDetailScreen, CaseAttachmentEntity | Done Pass 2 |
| MIME type on attachments | CaseAttachmentEntity, AttachmentDraft | Done Pass 2 |
| Entity aliases | EntityProfileEntity, EntityDraft, dialogs | Done Pass 2 |
| Lead category tags | LeadEntity, LeadDraft, dialogs | Done Pass 2 |
| Lead and entity delete | DAOs, Repository, ViewModel | Done Pass 2 |
| Lead and entity edit | DAOs, Repository, ViewModel | Done Pass 2 |
| Attachment caption edit and delete | AttachmentDao, Repository, ViewModel | Done Pass 2 |
| Explicit Room migrations (2-3, 3-4, 4-5, 5-6) | FieldbookDatabase | Done Pass 2 |
| Room schema export with KSP arg | FieldbookDatabase, build.gradle.kts | Done (regression caught Pass 2) |
| Schema snapshots committed (v4, v5, v6) | app/schemas/ | Done Pass 2 |
| Bulk vault export (P4) | CasePackageExporter, DashboardViewModel, DashboardScreen | Done Pass 3 |

### 3.2 Regression Fixed in Pass 2

**`exportSchema = false` (FieldbookDatabase.kt):** Codex reverted `exportSchema` from `true` to `false` during the feature additions. The KSP `room.schemaLocation` argument was still in `build.gradle.kts`, but with `exportSchema = false`, KSP does not write the schema file. The v6 schema did not exist. Fixed to `true`. v6.json generated and committed.

### 3.3 Fixes in Pass 3

**`DashboardViewModelFactory` used `as T` with `@Suppress("UNCHECKED_CAST")`:** Changed to `modelClass.cast(DashboardViewModel(repository))` to match the pattern in all other ViewModel factories (`CasesViewModelFactory`, `CaseDetailViewModelFactory`, `ArchiveViewModelFactory`).

### 3.4 Outstanding Items

| Item | Priority | Notes |
|---|---|---|
| AddEntityDialog: entityType and confidence are hardcoded | Medium | `entityType = EntityType.ORGANIZATION` and `confidence = ConfidenceLevel.PROBABLE` are hardcoded in the dialog. User cannot set PERSON, COMPANY, VEHICLE, or VERIFIED. Add type selector and confidence selector to AddEntityDialog. |
| CasesViewModel.addCase: no error surface | Low | The only ViewModel that still has a bare `viewModelScope.launch { repository.addCase(draft) }` without `runCatching` or a `userMessage` field. In practice, `insertCase` with REPLACE strategy rarely throws. Add `runCatching` and a message state for consistency. |
| P7: Encrypted storage | Low | SQLCipher. Significant implementation risk. Manual review required before implementation. |
| LeadDraft.tags has no default value | Low | If a new call site creates a `LeadDraft` without specifying `tags`, it will fail to compile. Easy to catch at compile time. Add `tags: String = ""` default. |
| EntityDraft.aliases has no default value | Low | Same issue. Add `aliases: String = ""` default. |

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

Three export types: case master note, session log, entity note. Plus bulk vault ZIP.

| Feature | Status |
|---|---|
| YAML frontmatter with Case-ID, Status, Tags, Save-Path | Working |
| Lead log table with Tags column | Working |
| Entity wiki links with corrected underscored filenames | Working |
| Known Aliases section as bullet list | Fixed Pass 2 |
| Attachment MIME type in export | Working |
| GPS coordinates in attachment block | Working |
| EXIF timestamp in attachment block | Working |
| Session log attachment block with GPS and timestamp | Working |
| Source appendix | Working |
| Bulk vault ZIP (all cases, folder-per-case structure) | Working (Pass 3) |

---

## 6. Vulnerability Surface

| Item | Risk | Status |
|---|---|---|
| HTTP logging in release builds | Medium | Fixed Pass 1 (NONE in release) |
| Wayback API no timeout | Medium | Fixed Pass 1 (15s connect + read) |
| Enum deserialization crash on unknown value | Medium | Fixed Pass 1 (runCatching with defaults) |
| `fallbackToDestructiveMigration` | Critical | Removed in hardening pass |
| Local data unencrypted at rest | Medium | Android FBE provides baseline. SQLCipher path documented in Section 3.4. |
| Wayback lookups visible to archive.org | Informational | Inherent to the API. No mitigation available client-side. |
| Bulk export silently skips inaccessible attachments | Low | If an attachment URI is no longer accessible (file deleted externally), its entry is skipped in the ZIP without error. This is intentional for resilience. |

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

## 8. Bulk Export Implementation Notes (Pass 3)

`CasePackageExporter.writeVaultPackage` takes four suspend lambdas for data access (`leadsFor`, `entitiesFor`, `attachmentsFor`) so the exporter stays decoupled from Room. `DashboardViewModel.exportAllCases` runs on `Dispatchers.IO` and uses `Flow.first()` snapshots from existing repository flows. No new DAO queries were added.

ZIP structure: `caseFolderName/masterNoteName.md` + `caseFolderName/attachments/1_filename.jpg`. This matches the `10_Investigations/` folder layout of the Obsidian vault for direct drop-in import.

The `writeAttachmentEntry` private helper is shared between `writeCasePackage` (single case) and `writeVaultPackage` (all cases), eliminating the code duplication that existed before this pass.

---

## 9. Final Assessment

The codebase is in production-grade shape for a single-operator field tool. The migration chain is complete and explicit. The export surface now covers individual case notes, session logs, entity notes, case packages, and full vault ZIPs. EXIF capture, edit/delete controls, and user feedback are all working. The two highest-priority items remaining are the entity dialog type/confidence selector gap (medium) and the encrypted storage path (low, deliberate deferral).

---

*Pass 3 completed 2026-04-19.*
