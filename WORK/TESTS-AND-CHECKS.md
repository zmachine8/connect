# Tests and checks

Quality is layered: fast automated checks run on every pull request; feature behaviour is also tested on an emulator or device.

## Required before requesting review

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

- `testDebugUnitTest`: runs local JVM unit tests.
- `lintDebug`: finds Android correctness, performance and accessibility risks.
- `assembleDebug`: proves the debug application compiles and packages.

GitHub Actions runs the same commands. Local success reduces wasted review time; CI proves the result works in a clean environment.

## Test types

| Type | Purpose | Examples in Connect |
|---|---|---|
| Unit | Pure logic and state changes | validation, deterministic chat ID, ViewModel transitions |
| Repository | Data contract independent of UI | users returned, message stored, permission error surfaced |
| Compose UI | Visible behaviour and user actions | error text, disabled Send, navigation click |
| Integration | Several real layers together | ViewModel with Firebase emulator repository |
| Manual | Device-specific behaviour | camera permissions, rotation, keyboard, photo capture |
| Security | Access rules reject unauthorised actions | nonparticipant cannot read a chat |

## Per-feature checklist

- [ ] Happy path works.
- [ ] Loading state is visible where work takes time.
- [ ] Empty state is understandable.
- [ ] Error state explains what happened and offers recovery when possible.
- [ ] Invalid input is rejected safely.
- [ ] Rotation or recreation does not destroy important screen state.
- [ ] Back navigation is sensible.
- [ ] The feature was tried on an emulator/device.
- [ ] A regression test exists when practical.

## Bug severity

- **Critical:** privacy/security loss, data exposed, app cannot start.
- **High:** core MVP flow unavailable or data lost.
- **Medium:** feature partly fails with a workaround.
- **Low:** cosmetic or minor usability problem.

Never close a bug only because code changed. Repeat its reproduction steps and record the result.
