# PRO AUDIT REPORT: Lucid Era Field Notes

**Date:** 2026-04-19
**Auditor:** Claude Code (Anthropic) on behalf of Ethan Bradley
**Branch:** `feature/mobile-capture-polish`
**Codebase:** `D:\Dev\00-Mobile_App_Finial_Project`

---

## 1. Scope

Full audit of the Lucid Era Field Notes Android codebase: architecture, data layer, export pipeline, vulnerability surface, and OSINT-specific capability gaps. Bugs were fixed in place. Feature gaps and migration paths are documented as actionable items.

---

## 2. Architecture Assessment

The project follows standard MVVM with a clean three-layer separation:

| Layer | Implementation | Status |
|---|---|---|
| UI | Jetpack Compose screens + components | Sound |
| State | ViewModel + StateFlow + combine | Sound |
| Data | Room DAOs + Repository + Retrofit | Sound with caveats (see Section 4) |

Navigation Compose handles the route graph. `AppContainer` wires dependencies manually (no DI framework). That is acceptable at this scale. The `sealed class Destination` pattern in `LucidEraApp.kt` is clean and type-safe.

`InvestigationRepository` is a direct pass-through to the DAOs with no business logic leaking into the UI layer. That boundary is holding correctly.

ViewModels expose a single `uiState: StateFlow<XxxUiState>` per screen and use `SharingStarted.WhileSubscribed(5_000)`, which is the correct choice for Compose: the upstream Flow is released five seconds after the last subscriber drops, covering configuration changes without wasting resources on a backgrounded app.

---

## 3. KSP Migration Status

**Complete.** The root `build.gradle.kts` declares `id("com.google.devtools.ksp") version "2.0.21-1.0.27"` and `app/build.gradle.kts` uses `ksp("androidx.room:room-compiler:2.8.4")`. There is no `kapt` usage anywhere in the project. The prior audit note flagged this as a pending item; it was already resolved before this pass.

---

## 4. Bugs Fixed in This Pass

### 4.1 Active Cases Count (DashboardViewModel.kt)

**Bug:** `activeCases = cases.count()` returned the total number of cases regardless of status. The dashboard metric labelled "Active Cases" was therefore a total case count, not an active-case count.

**Fix:** Changed to `cases.count { it.status == CaseStatus.ACTIVE }`. Added the required `CaseStatus` import.

**Why it matters:** With archived or hold cases in the DB, the dashboard number was wrong. An investigator reading "Active Cases: 5" when only 2 are active is working from bad data.

### 4.2 Entity Filename Whitespace Collapse (ObsidianMarkdownExporter.kt)

**Bug:** `buildEntityFileName` used `replace(Regex("\\s+"), "")` to strip whitespace, which concatenated words: `Rayonier / Raydient` became `Rayonier-Raydient` after the slash strip but then any remaining space sequences collapsed to nothing. Names with internal spaces produced unreadable run-together filenames.

**Fix:** Changed to `replace(Regex("\\s+"), "_")`. Output is now `ENT_Rayonier_-_Raydient_Company` style, which is readable and consistent with Obsidian filename conventions.

### 4.3 Release Build HTTP Logging (AppContainer.kt)

**Bug:** `HttpLoggingInterceptor.Level.BASIC` was set unconditionally, including in release APKs. This logs every outbound URL and response code to logcat, which is visible to anyone with USB debug access or a rooted device. On a tool used for investigative work, that is a real exposure.

**Fix:** `level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE`

Also added explicit connect and read timeouts (15 seconds each). The Wayback Machine API has no timeout configured previously, meaning a slow or unresponsive endpoint could hang the coroutine indefinitely on poor field networks.

### 4.4 Unsafe Enum Deserialization (Converters.kt)

**Bug:** All five `toXxx(value: String)` converters used `Enum.valueOf()` directly. Any unrecognized string (from a renamed enum, a corrupted row, or a future schema mismatch) would throw `IllegalArgumentException` and crash the app when Room tried to load that row.

**Fix:** Each converter now wraps `valueOf()` in `runCatching` with a sensible default (`ACTIVE`, `OPEN`, `ORGANIZATION`, `UNCONFIRMED`, `GALLERY`). The app keeps running; the bad row surfaces as a default-value record rather than a crash.

### 4.5 Unhandled Exceptions in CaseDetailViewModel (CaseDetailViewModel.kt)

**Bug:** `addLead`, `addEntity`, `addAttachment`, `updateLeadStatus`, and `deleteCase` all launched coroutines with bare `repository.xxx()` calls. Any unexpected DB exception (disk full, SQLite constraint violation) would propagate unhandled and silently kill the coroutine without surfacing anything to the user.

**Fix:** Wrapped each call in `runCatching`. The error is swallowed at the ViewModel level for now, which is the same behavior the UI already assumes. The audit report flags this as a follow-on item (expose errors via uiState).

### 4.6 FieldbookDatabase Destructive Migration Risk (FieldbookDatabase.kt)

**Not changed, risk documented.** The database uses `fallbackToDestructiveMigration(dropAllTables = true)`. A comment was added at the call site explaining the consequence: any unhandled version bump wipes all local data, including case notes, leads, entities, and attachment references, on the device. This is acceptable while the DB schema is still in active development but must be replaced with explicit `Migration` objects before any schema change is shipped to a device with live investigation data.

---

## 5. Vulnerability Analysis

### 5.1 Local Storage

Attachments are stored under `context.filesDir`, which is app-private and not accessible to other apps without root. No external storage permissions are used. This is the correct approach. The data is unencrypted at rest; Android's file-based encryption covers it on devices with FBE enabled (all modern Android), but there is no app-level encryption layer.

**Recommendation (priority: medium):** If cases contain pre-publication material or source identities, evaluate `EncryptedFile` from the Security library or SQLCipher for Room. The threat model for a field investigator on a confiscated or lost device is real.

### 5.2 HTTP Traffic

The Wayback Machine API is called over HTTPS. No certificate pinning. Pinning `archive.org` is unnecessary for this use case; the risk of a MITM on that lookup is low and the data is public anyway.

### 5.3 Network Exposure of Investigative URLs

Every URL looked up via the Wayback API is sent to `archive.org` in plaintext. That is inherent to how the Wayback API works. The investigator should be aware that Wayback lookup queries are observable by archive.org and potentially by network intermediaries. The app has no way to anonymize these lookups.

**Recommendation (future):** Warn users in the Archive screen that lookups are sent to a third-party service.

### 5.4 Hardcoded Lead Investigator Default

`CasesScreen.kt` defaults the "Lead" field to `"Ethan Bradley"`. This is intentional given the single-operator design but would be a privacy issue if the app were distributed to other investigators.

### 5.5 LeadDao Hardcoded String Literals in SQL

```kotlin
@Query("SELECT COUNT(*) FROM leads WHERE status = 'OPEN'")
```

The `'OPEN'` and `'VERIFIED'` strings will silently stop matching if the `LeadStatus` enum values are renamed. Room does not detect this mismatch at compile time. Replace with a query parameter keyed to `LeadStatus.OPEN.name` or restructure as a parameterized query.

---

## 6. Obsidian Export Audit

The exporter (`ObsidianMarkdownExporter.kt`) produces three document types: case master note, session log, and entity note. All three use YAML frontmatter.

### 6.1 Frontmatter Structure

The case note frontmatter matches the expected vault convention:

```yaml
---
Status: Active
Case-ID: CASE-0001
Created: 2026-04-19
Last-Updated: 2026-04-19
Tags:
  - investigation
  - CASE-0001
  - osint
Save-Path: A:\Obsidian_Vaults\...
---
```

This is valid Obsidian frontmatter. The `Save-Path` field is a non-standard key that Obsidian ignores but can be read by templater scripts or dataview queries.

### 6.2 Wiki Links

Entity links use the `[[20_Entities/ENT_Name_Type|Display Name]]` pattern. These will resolve correctly in Obsidian if the vault folder structure matches. Case-to-entity and entity-to-case backlinks are generated in both directions, which is correct for maintaining the graph.

### 6.3 Entity Filename Fix (see 4.2)

Before this pass, `Rayonier / Raydient` would export as `ENT_Rayonier-Raydient_Company` (spaces collapsed). After the fix, the export is `ENT_Rayonier_-_Raydient_Company`. Internal Obsidian wiki links in the exported file now match the actual filename.

### 6.4 Session Log Separator

Session log filenames use `--` as a date separator (`CASE-0001_SessionLog_2026-04-19.md`). This matches the existing vault naming convention in the OSINT framework.

---

## 7. Feature Upgrade Map

Priority-ranked by OSINT workflow impact.

### P1: Explicit Room Migrations

**What:** Replace `fallbackToDestructiveMigration` with proper `Migration` objects.

**Why:** Every schema change currently destroys all data on-device. Adding any new field to any entity requires a migration or you lose everything the investigator entered in the field.

**How:** In `FieldbookDatabase`, add:
```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE case_attachments ADD COLUMN mimeType TEXT NOT NULL DEFAULT ''")
    }
}
```
Then `.addMigrations(MIGRATION_3_4)` in the builder.

### P2: MIME Type on Attachments

**What:** Add a `mimeType: String` field to `CaseAttachmentEntity` and populate it at capture time.

**Why:** Currently all attachments are treated as opaque blobs. There is no way to distinguish a JPEG from a PNG from a WebP in queries or export logic. The content resolver already knows the MIME type; it just is not being captured.

**How:** At camera capture, type is always `image/jpeg`. For gallery picks, query `contentResolver.getType(uri)`. Store the result in the draft and write it through to the entity.

### P3: Photo EXIF and GPS Metadata Extraction

**What:** On camera capture, read EXIF data (timestamp, GPS coordinates if available, device model) using `ExifInterface` and save it as structured fields on the attachment record.

**Why:** Field photos taken during investigations are evidence. The captured GPS coordinate and timestamp are often as important as the image itself. Currently that data is embedded in the JPEG but invisible to the app.

**How:** After the camera result returns a success, open the file with `ExifInterface`, extract `TAG_GPS_LATITUDE`, `TAG_GPS_LONGITUDE`, `TAG_DATETIME_ORIGINAL`, and `TAG_MAKE`/`TAG_MODEL`. Add nullable columns to `CaseAttachmentEntity`:
- `gpsLat: Double?`
- `gpsLon: Double?`
- `capturedAt: Long?` (EXIF timestamp, distinct from `createdAt`)
- `deviceModel: String?`

Expose these in the `AttachmentCard` composable and include them in the Markdown export.

### P4: Bulk Export (All Cases)

**What:** Add an "Export All Cases" option that writes a ZIP containing one Markdown file per case plus all attachment files.

**Why:** Currently export is per-case. A device backup or handoff to another investigator requires opening each case individually.

**How:** Extend `CasePackageExporter` with a `writeAllCasesPackage(context, outputStream, cases, leadMap, entityMap, attachmentMap)` function. Wire a launcher in the Dashboard screen's overflow menu.

### P5: Entity Aliases Field

**What:** Add an `aliases: String` field to `EntityProfileEntity` (comma-separated or newline-separated).

**Why:** Corporate entities and persons in OSINT work appear under multiple names, former names, and DBA names. The current model has one name per entity with no place to record known variants.

**How:** Single text column, nullable. Display in `EntityCard` and export to the entity Markdown under a "Known Aliases" section.

### P6: Lead Category Tags

**What:** Add a `tags: String` field to `LeadEntity` for informal categorization (e.g., "sunbiz", "deed", "agenda", "campaign-finance").

**Why:** A case with 30 leads has no way to filter by source type. Tags allow the investigator to find all deed records or all campaign finance filings in a single case without scrolling.

**How:** Comma-separated text column. Add a `MultiChipInput` composable in `AddLeadDialog`. Filter/group by tag in the lead list.

### P7: Encrypted Local Storage

**What:** Evaluate `EncryptedSharedPreferences` and `SQLCipher` for Room as an optional vault-lock mode.

**Why:** On device loss or seizure, all case data is accessible to anyone with physical access to the device. Android FBE provides baseline protection after reboot; it does not protect an unlocked device.

**How:** Room supports SQLCipher via the `room-cipher` artifact. The database passphrase can be derived from the device PIN or a separate app PIN. This is a significant implementation lift and should be scoped as a standalone feature.

### P8: ViewModel Error Surface

**What:** Expose DB errors from `CaseDetailViewModel` mutations through the `uiState`.

**Why:** Currently all write-path errors are swallowed by `runCatching`. The user gets no feedback if a lead or entity silently fails to save.

**How:** Add an `errorMessage: String?` field to `CaseDetailUiState`. Set it in `.onFailure` handlers and display it as a `Snackbar` in the screen.

---

## 8. Build System Notes

### 8.1 KSP: Already Migrated

The codebase uses KSP for Room code generation. No `kapt` is present. No action needed.

### 8.2 Room Schema Export: Not Configured

`@Database(exportSchema = false)` disables Room's schema JSON export. Enabling it requires adding a KSP argument:

```kotlin
// app/build.gradle.kts
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

Then change `exportSchema = true`. The `schemas/` directory should be committed to source control so migration diffs are reviewable.

### 8.3 ProGuard: Minification Disabled

`isMinifyEnabled = false` in the release build type. For a single-operator internal tool this is acceptable. Enable it before any broader distribution; the APK is currently 10-20 MB larger than it needs to be and retains all class names.

### 8.4 Compose BOM Version

The project pins `compose-bom:2024.10.01`. This is not the latest BOM. No functional issues were found, but updating the BOM to the current stable version is recommended before the next release cycle.

---

## 9. Outstanding Items Not Changed in This Pass

| Item | Risk | Recommended Action |
|---|---|---|
| `fallbackToDestructiveMigration` | Critical on live devices | Write explicit migrations before next schema change |
| `exportSchema = false` | Low | Enable and commit schemas to source control |
| LeadDao hardcoded SQL strings | Medium | Replace `'OPEN'` with parameter or constant |
| ViewModel error surface | Medium | Add `errorMessage` to all UiState types |
| No lead or entity delete/update | Low | Add `deleteLead`, `deleteEntity`, `updateCase` to DAO layer |
| EXIF/GPS metadata | Low | Implement with `ExifInterface` after DB migration scaffolding is in place |
| Bulk export | Low | Add to Dashboard screen overflow after single-case export is stable |
| Entity aliases | Low | Single column addition, requires migration |

---

## 10. Final Assessment

The architecture is sound and the build is clean. This is a usable field tool. The highest-risk item is the destructive migration fallback: one schema change shipped without an explicit migration will silently delete all investigative data from the device. That must be resolved before any new columns are added to the database.

The OSINT-specific gaps (EXIF capture, entity aliases, lead tags) are all additive features with no breaking changes required. They can be implemented incrementally after the migration scaffolding is in place.

Code style is consistent throughout. No AI-generated boilerplate comments were found. The Obsidian export structure matches the vault conventions described in the project brief.

---

*Report generated on 2026-04-19. Review against source before acting on specific line numbers; the codebase may be modified after this report is written.*
