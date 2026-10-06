# GitHub workflow

The course asks for frequent commits and merges without an unnecessarily complex branching strategy. Use short-lived branches and merge them promptly.

## One task from start to finish

1. Choose an assigned GitHub Issue with acceptance criteria.
2. Update local `main`:

   ```bash
   git switch main
   git pull --ff-only
   ```

3. Create a focused branch:

   ```bash
   git switch -c feature/users-empty-state
   ```

4. Work in small steps. Inspect changes before committing:

   ```bash
   git status
   git diff
   git add <specific-files>
   git commit -m "Add empty state to users screen"
   ```

5. Run the checks from [TESTS-AND-CHECKS.md](TESTS-AND-CHECKS.md).
6. Bring in recent `main` before review if necessary:

   ```bash
   git fetch origin
   git rebase origin/main
   ```

7. Push and open a pull request:

   ```bash
   git push -u origin feature/users-empty-state
   ```

8. Add a teammate as reviewer. The author remains responsible for responding and repairing failures.
9. Merge only after CI passes and review is complete.
10. Delete the merged branch and pull the updated `main`.

## Naming

Branches:

```text
feature/chat-input
fix/blank-message
test/user-repository
docs/firebase-setup
```

Good commits:

```text
Add placeholder users screen
Model loading and error user states
Test blank chat messages
Document local setup
```

Avoid `changes`, `stuff`, `work`, `final` and commits that mix several unrelated purposes.

## Pull-request author checklist

- [ ] The PR handles one coherent issue.
- [ ] The description says what, why and how it was checked.
- [ ] Relevant documentation changed with the code.
- [ ] No generated output or secret is included.
- [ ] CI passes.
- [ ] A teammate has reviewed it.

## Reviewer checklist

- [ ] Read the linked issue and acceptance criteria.
- [ ] Inspect the changed files, not only screenshots.
- [ ] Check architecture boundaries and naming.
- [ ] Check tests and important failure states.
- [ ] Request concrete changes where needed.
- [ ] Approve only a version you actually reviewed.

Do not approve your own pull request even if GitHub technically permits it.

## Recommended `main` protection

- Require pull requests.
- Require one approval.
- Require Android CI.
- Block force pushes and direct pushes.
- Dismiss approval when new changes are pushed, if available.
