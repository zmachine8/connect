# Course-aligned roadmap

Only weeks 1–3 have published slide files at the time this foundation was created. Weeks 4–16 below follow the official published lecture topics and may need adjustment when their slides appear.

| Week | Course topic | Connect outcome |
|---:|---|---|
| 1 | Ideas and prototyping | Team, scoped idea, GitHub repository |
| 2 | Kotlin for mobile apps | Models, null safety, immutable state and testable rules |
| 3 | Compose and architecture | Shared app base, layers, prototype screens, CI and handbook |
| 4 | Layouts and Material Design | Implement approved visual system and responsive screen layouts |
| 5 | Interaction, forms and navigation | Real forms, validation and navigation behaviour |
| 6 | State management | ViewModel/observable state for all screens; Stage 2 report |
| 7 | Device APIs: sensors | Camera/permission spike only if it will not delay text MVP |
| 8 | Midterm presentation | Demonstrate stages 1–2 and working UI flow |
| 9 | Data storage | Finalise user/chat/message data model and persistence contract |
| 10 | APIs and Firebase | Firestore repositories and persistent one-to-one messages |
| 11 | Authentication | Firebase Authentication and user profiles/directory |
| 12 | AI features | Optional; do not add AI unless the MVP is healthy and scope is approved |
| 13 | Testing and polish | Automated tests, debugging, accessibility and edge cases |
| 14 | MVP and peer testing | Complete text MVP, peer test, then finish photo sharing |
| 15 | Release and poster | Release build, signing plan, final documentation and poster |
| 16 | Demo and poster session | Stable final demonstration and evidence |

## Stage gates

### Foundation gate

- [ ] Fresh clone builds and runs.
- [ ] CI passes.
- [ ] Team workflow and Lead assignments are recorded.

### Text-MVP gate

- [ ] Account creation/login works.
- [ ] Authenticated user can discover registered users.
- [ ] Two participants can exchange persistent text messages.
- [ ] Unrelated account cannot access the conversation.

### Photo gate

- [ ] Text MVP is stable first.
- [ ] Camera permission denial is handled.
- [ ] Captured photo can be sent and viewed by both participants.
- [ ] Upload failure has a recovery path.

### Release gate

- [ ] Critical/high defects are closed.
- [ ] Privacy rules have negative security tests.
- [ ] Release build and demo path are repeatable.
- [ ] Poster, credits and documentation are complete.
