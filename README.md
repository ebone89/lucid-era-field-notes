# Lucid Era Field Notes

Lucid Era Field Notes is an Android app built around the Lucid Era Investigations workflow. It is designed as a field capture tool for case intake, lead logging, entity tracking, archive checks, and Markdown export into an Obsidian vault.

## Core Features

- Case tracking with case code, essential question, subject, classification, and publication threshold
- Lead logging with status changes for open, verified, archived, and deferred items
- Entity tracking tied to individual investigations
- Wayback Machine lookup for archived snapshots
- Speech-to-text for lead, entity, and attachment note entry
- Camera and gallery attachments stored with each case
- Obsidian-ready Markdown export for case notes, session logs, and entity notes
- Android share-sheet support for sending exported notes to Drive, email, or other apps

## Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- ViewModel
- Room
- Retrofit
- Coroutines

## Project Layout

- `app/` Android Studio module
- `documentation.docx` project write-up
- `screenshots/` submission screenshots

## Build

Open the project root in Android Studio, allow Gradle to sync, and run on an emulator or Android device.

Verified locally:

- `assembleDebug`
- `assembleRelease`

## Current Scope

The app is meant to support field capture and structured handoff into the Lucid Era Obsidian workflow. It does not directly sync with the Windows vault; instead, it exports Markdown notes that can be reviewed and merged into the vault.
