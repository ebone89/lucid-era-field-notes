Lucid Era Fieldbook

Android final project tailored to the Lucid Era Investigations OSINT workflow.

What the app does
- Creates and stores investigation cases with an essential question, primary subject, and publication threshold.
- Tracks raw leads separately from verified findings logic.
- Stores entity profiles related to a case.
- Looks up archived snapshots for public URLs through the Wayback Machine API.
- Uses Jetpack Compose, Navigation Compose, ViewModel, Room, Retrofit, and coroutines.

Project structure
- app/ contains the Android Studio project module.
- documentation.docx contains the required project explanation.
- screenshots/ is where final emulator or device screenshots should be placed before submission.

Suggested screenshots to capture
- Dashboard with case metrics
- Cases screen with at least one created case
- Case detail showing leads and entities
- Archive lookup screen showing a successful Wayback result

Build note
- Open the folder in Android Studio.
- Let Gradle sync the project dependencies.
- Run on an emulator or physical Android device.

Known gap from this environment
- I could not run a full Gradle build in this terminal because Gradle is not installed globally here and the project was created from scratch in an empty folder.
