# Connect

Connect is a native Android prototype for private one-to-one communication. Its MVP path is:

1. account and login;
2. registered-user directory;
3. private text chat;
4. camera-captured photo sharing.

Feeds, likes, comments, stories, calls, groups, recommendations, advertising and monetisation are outside the scope.

## Current state — Week 3

The repository currently contains the shared project foundation:

- Kotlin and Jetpack Compose Android application;
- Login → Users → Chat prototype navigation;
- fake local users and messages, so the UI can be developed before Firebase;
- UI → ViewModel → Repository separation;
- loading, success, error and retry state structure for the user list;
- one unit test;
- GitHub Actions build, lint and test checks;
- team workflow and Lead-role instructions in [`WORK/`](WORK/README.md).

Authentication, Firestore, Firebase Storage and the camera are intentionally not implemented yet.

## Run locally

Requirements: Android Studio, JDK 17 and an Android SDK with API 36.

1. Clone the repository.
2. Open its root folder in Android Studio.
3. Let Gradle sync.
4. Create or select an emulator running Android 8.0/API 26 or newer.
5. Run the `app` configuration.

Command-line checks:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Architecture

```text
Compose UI → ViewModel → Repository → local/remote data source
```

Screens must not access Firebase directly. See [`WORK/ARCHITECTURE.md`](WORK/ARCHITECTURE.md).

## Team

- Raigo Leesment
- Mairon Mihkelsoo
- Robin Murumets
- Kermo Mätlik
- Reimo Zukker

Stage assignments belong in [`WORK/ROLE-ROTATION.md`](WORK/ROLE-ROTATION.md).
