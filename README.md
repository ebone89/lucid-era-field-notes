# Lucid Era Field Notes

Android field capture tool for the Lucid Era Investigations workflow. Built to replace paper notes and scattered phone screenshots with a structured, exportable record that feeds directly into the Obsidian vault at `A:\Obsidian_Vaults\Main-Notes`.

The app is not a sync client. It is a field log: capture fast, export clean, merge on the desktop.

## What It Does

- **Case tracking**: each case holds a code, essential question, primary subject, classification, publication threshold, and working summary.
- **Lead logging**: record sources with a live URL, optional archive link, and status (Open, Verified, Archived, Deferred).
- **Entity profiles**: tag persons, companies, domains, organizations, addresses, and assets to a case with a confidence rating.
- **Wayback Machine lookup**: check whether a page has a saved snapshot and file it as a lead in one step.
- **Speech-to-text entry**: dictate leads, entity notes, and photo captions in the field.
- **Camera and gallery attachments**: photos are stored in app-owned persistent storage and referenced in the exported Markdown.
- **Obsidian-ready export**: case notes, session logs, and entity notes export as `.md` files with YAML frontmatter that matches the vault structure.
- **Share sheet support**: send any export to Drive, email, or a Markdown editor without leaving the app.

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow |
| Persistence | Room (KSP) |
| Network | Retrofit + OkHttp |
| Async | Coroutines |
| Image | Coil 3 |

## Project Layout

```
app/                       Android module
  src/main/java/
    data/                  Repository, DAOs, entities, export, import, network
    ui/                    Composables, ViewModels, theme
  src/main/res/            Resources, file_paths.xml (FileProvider)
documentation.docx         Project write-up
screenshots/               Submission screenshots
```

## Build

Open the project root in Android Studio. Gradle sync is required before first run. Build targets verified:

- `assembleDebug`
- `assembleRelease`

Minimum SDK: 26 (Android 8.0). Target SDK: 36.

## Export Format

Exported case notes and entity files are structured to drop into the vault without reformatting. The YAML frontmatter sets `Status`, `Case-ID`, `Save-Path`, and `Tags` to match the existing vault convention. Session logs are stamped with the current date and filed separately from the master case note.

## Vault Integration

The export path embedded in each case record should point to the correct Obsidian folder under:

```
A:\Obsidian_Vaults\Main-Notes\03_Organizations\03_Lucid_Era_Group\031-Lucid_Era_Investigations\10_Investigations\
```

Copy or move exported `.md` files there after each field session. Attachment files referenced in the Markdown still live on the phone; copy them to the `attachments/` subfolder in the case folder if you want vault-local copies.

## Current Limits

- No direct sync with the Windows vault.
- No attachment delete or edit after initial capture.
- No entity-to-entity relationship links.
- No EXIF or GPS metadata extraction from photos.
