# Quality Lead

## Accountable for

- acceptance criteria and the testing strategy;
- reproducible defect reports;
- evidence that requirements are met;
- making quality visible throughout development, not only at the end.

## Current duties

- [ ] Add acceptance criteria before an issue becomes `Ready`.
- [ ] Check that pull requests explain how they were tested.
- [ ] Maintain unit, integration, UI, manual and security test lists.
- [ ] Reproduce bugs before prioritising them.
- [ ] Confirm that repaired bugs no longer reproduce.
- [ ] Record tested device/API combinations.
- [ ] Preserve screenshots or results needed as report evidence.

## Example acceptance criteria

- Given a fresh installation, when the app launches, then the Connect prototype opens without crashing.
- Given the users screen, when a user is selected, then the correct chat title is displayed.
- Given a blank message, when Send is pressed, then no message is added.
- Given a clean clone, when the documented checks run, then all checks pass.

## Later Firebase security check

Use three accounts: participant A, participant B and unrelated account C. A and B may access their conversation; C must not be able to read or write it. Include negative tests—successful participant tests alone do not prove privacy.
