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

The tests run against the demo-connect emulator project. They cover access restrictions, participant membership, sender identity and message payload validation. They do not deploy rules to the development Firebase project.

## Manual checks

- Register, sign in and sign out; return from Create account to Sign in.
- Edit a display name and verify another account sees the update.
- Use two accounts to send and receive text, photos, captions, documents and location.
- Reject empty text and oversized documents; cancel or remove attachment selections.
- Rotate forms and chat with unsent input; check compact layouts with the keyboard open.
- Follow the process-recreation procedure in [state management](state-management.md).

Emulator feedback verified rotation and landscape improvements. Process recreation, failure handling, large fonts and broader device coverage remain additional verification work.

See [integration guide](../CONNECT-INTEGRATION.md) for setup and prototype limits.
