# Testing Connect

## Android verification

From the repository root on Arch Linux:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest lintDebug assembleDebug
```

All three checks passed during the latest reported local verification. The existing JVM test covers the fake-user repository; it does not cover every screen or state transition. Android CI runs the project checks separately from Firestore rules tests.

## Firestore rules

```bash
cd rules-tests
npm ci
JAVA_HOME=/usr/lib/jvm/java-21-openjdk npm test
```

The tests run against the demo-connect emulator project. They cover signed-out and cross-account access, direct and group participant membership, contact-request creation and acceptance, sender identity, message payload validation, and editing or migrating a legacy profile without a username. They do not deploy rules to the development Firebase project.

## Manual checks

- Register with an unused username; confirm a username already owned by another account is rejected; sign in and sign out; return from Create account to Sign in.
- After signing out, sign in as a second account and confirm the first account's profile, people data and conversations are no longer visible.
- Edit a display name and verify another account sees the update.
- Use two accounts to send and receive text, photos, captions, documents and location.
- Reject empty text and oversized documents; cancel or remove attachment selections.
- Rotate forms and chat with unsent input; check compact layouts with the keyboard open.
- Follow the process-recreation procedure in [state management](state-management.md).

Emulator feedback verified rotation and landscape improvements. Process recreation, failure handling, large fonts and broader device coverage remain additional verification work.

See [integration guide](../CONNECT-INTEGRATION.md) for setup and prototype limits.
