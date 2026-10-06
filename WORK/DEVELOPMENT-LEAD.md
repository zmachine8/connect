# Development Lead

## Accountable for

- technical direction and architecture;
- Gradle, CI and build readiness;
- integration of team contributions;
- preventing secrets and generated outputs from entering Git;
- documenting technical decisions and setup.

## Current duties

- [ ] Make sure a fresh clone opens and builds.
- [ ] Keep `main` green.
- [ ] Preserve UI → ViewModel → Repository → data-source boundaries.
- [ ] Split work so teammates rarely edit the same file.
- [ ] Review architecture-changing pull requests.
- [ ] Merge small completed work frequently.
- [ ] Keep README setup steps accurate.

## Development co-leads

For a five-person stage, this role can be shared:

- **Architecture co-lead:** Kotlin, Compose, ViewModels, repositories and dependency direction.
- **Integration co-lead:** GitHub, CI, Gradle, merging and reproducible builds.

Both remain accountable for technical readiness and should coordinate before changing shared build or navigation files.

## Never allow

- Compose screens accessing Firebase directly;
- committed passwords, signing keys, API secrets, private user data or `google-services.json`;
- committed APK/AAB/JAR files, build directories or Android Studio settings;
- unrelated features bundled into one large pull request;
- known build failure on `main` without an immediate repair issue and owner.
