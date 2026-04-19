Lucid Era Field Notes

Android project built around the Lucid Era Investigations workflow.

What the app does
- Creates and stores investigation cases with an essential question, subject, classification, and publication threshold.
- Tracks leads, linked entities, and case attachments in a local Room database.
- Supports field capture with speech-to-text for lead, entity, and photo-note entry.
- Supports camera and gallery attachments tied to a case.
- Exports case notes, session logs, and entity notes as Obsidian-ready Markdown.
- Checks archived snapshots for public URLs through the Wayback Machine API.

Project structure
- app/ contains the Android Studio project module.
- documentation.docx contains the project write-up.
- screenshots/ is for submission screenshots.

Suggested screenshots to capture
- Dashboard with current case counts
- Cases screen with the seeded investigations
- Case detail showing leads, entities, and attachments
- Archive lookup screen showing a successful Wayback result

Build note
- Open the project root folder in Android Studio.
- Let Gradle sync dependencies.
- Run on an emulator or physical Android device.

Verification
- Debug build completed successfully with assembleDebug.
- Release build completed successfully with assembleRelease.
