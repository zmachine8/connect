# Connect: Course and Project Progress Report

**Course:** Mobile Application Development, University of Tartu  
**Team:** Raigo Leesment, Mairon Mihkelsoo, Robin Murumets, Kermo Mätlik, and Reimo Zukker  
**Status date:** 10 November 2026  
**Current stage:** Stage 3 — Build and Integration  
**Stage 3 report deadline:** 19 November 2026

## Project overview

Connect is a native Android application for finding registered users and communicating privately. Users create accounts, find other people by username, make connections, and exchange messages in one-to-one or group conversations. They can share photos, documents, and approximate locations. The implementation uses Kotlin, Jetpack Compose, Firebase Authentication, Cloud Firestore, and Preferences DataStore.

## Course learning by week

The summaries below are based on the lecture slides in the course Sources folder and the course schedule. Weeks 1–10 are complete. Weeks 11–16 are upcoming as of the status date.

| Week and date | Session and connection to Connect | Status |
|---|---|---|
| **1 — 2 Sep** | **Mobile App Ideas & Prototyping.** Formed the team and repository, selected the Connect idea, and began defining users, needs, scope, and project roles. | Complete |
| **2 — 9 Sep** | **Kotlin for Mobile Apps.** Practised types, null safety, functions, data classes, enums, collections, lambdas, and asynchronous work used in the Android client. | Complete |
| **3 — 16 Sep** | **First Android App with Compose & App Architecture.** Covered Android project structure, Gradle, Compose, recomposition, state, and separation of UI, ViewModel, and data layers. Stage 1 Plan report due 17 Sep. | Complete |
| **4 — 23 Sep** | **Layouts & Material Design.** Applied mobile layout, visual hierarchy, and reusable Compose/Material components to the main screens. | Complete |
| **5 — 30 Sep** | **Interaction, Forms & Navigation.** Applied form input, touch interaction, feedback, and navigation to sign-in, People, Profile, and Chat. | Complete |
| **6 — 7 Oct** | **State Management.** Covered UI state versus persisted data, state hoisting, unidirectional data flow, `rememberSaveable`, ViewModels, and lifecycle-aware state. Stage 2 report due 11 Oct (updated deadline). | Complete |
| **7 — 14 Oct** | **Device APIs: Sensors.** Covered runtime permission states, lifecycle-safe sensor listeners, streams, one-shot results, and denial handling. Connect uses camera/gallery and approximate location; it does not claim a sensor-driven feature. [TaskTracker demo](https://courses.cs.ut.ee/2026/MAD/fall/Main/Lectures?action=download&upname=TaskTrack-W7-2026.zip). | Complete |
| **8 — 21 Oct** | **Midterm Project Presentation.** Presented the app and prototype. The team received 14/20. Feedback asked for clearer user-value and design rationale, stronger user research, and evidence from real users. | Complete |
| **9 — 28 Oct** | **Data Storage.** Covered data lifetime, DataStore for preferences, Room for local records, migrations, files for binary content, and repository/ViewModel flows. Connect uses DataStore for theme preferences and Firestore for cloud data; it does not use Room. [FieldNote demo](https://courses.cs.ut.ee/2026/MAD/fall/Main/Lectures?action=download&upname=FieldNote-W9-2026.zip). | Complete |
| **10 — 4 Nov** | **APIs & Cloud Backends (Firebase).** Covered cloud computing, HTTP/REST, API standards, OpenAPI, Retrofit, and cloud services. Connect uses Firebase Authentication and Firestore for accounts, profiles, connections, groups, and real-time chats. | Complete |
| **11 — 11 Nov** | **Authentication.** Upcoming. Connect already has Firebase email/password registration and sign-in, session restoration, sign-out, password reset, and profiles. Review the lecture and update the report afterward. | Upcoming |
| **12 — 18 Nov** | **AI Features on Android.** Upcoming. On-device ML Kit features such as summarization or message drafting are optional under the course project requirements. Decide after the lecture whether any feature is feasible and can be verified before the 19 Nov report deadline. | Upcoming; no AI feature claimed |
| **13 — 25 Nov** | **Testing, Debugging & Polish.** Planned: expand automated and manual checks, fix defects, and verify core user flows, permissions, error states, and device configurations. | Upcoming |
| **14 — 2 Dec** | **MVP Workshop & Peer Testing.** Planned: have peers try the MVP, record issues, and prioritize improvements. | Upcoming |
| **15 — 9 Dec** | **Finalisation, Release Build & Poster Preparation.** Planned: verify the release build, document known issues, and prepare the poster. | Upcoming |
| **16 — 16 Dec** | **Final Project Presentation: Demo & Poster Session.** Demonstrate the final application and present the poster. | Upcoming |

## Project stages

### Stage 1 — Plan

Stage 1 defined Connect's product idea, target users, requirements, technical approach, risks, acceptance criteria, and team workflow. The initial scope focused on account creation, finding another registered user, persistent one-to-one text messaging, and camera photo sharing. The team submitted the Plan report on **17 September**.

### Stage 2 — Design

Stage 2 described user journeys, navigation, screen structure, UI states, and the dark fantasy visual direction. The working Compose application served as the interactive prototype. The Design report covered sign-in and registration, People, Profile, Chat, and photo sharing. It also recorded that the team had not prepared a separate set of wireframes or run a participant usability study. The updated report deadline was **11 October**; the prototype was presented at the **21 October** midterm.

The midterm feedback recognized the working app and clear initial scope. It asked the team to explain why users would choose Connect, relate design choices to user needs, and conduct user testing. These findings guide the remaining work.

### Stage 3 — Build and Integration

Stage 3 implements and integrates persistent cloud data, authentication and profiles, Firebase services, device features, group communication, and state restoration. The report is due **19 November**.

The app currently supports Firebase registration and sign-in, Firestore profiles and relationships, username discovery, direct and group text chats, camera/gallery photo sharing, document sharing, approximate location sharing, and saved theme preferences. Stage 3 still needs evidence that the main flows work across accounts and that important failure states are handled clearly. The team will review the authentication lecture on 11 November and decide after the 18 November AI lecture whether an additional feature is practical.

### Stage 4 — Test, Polish, and Release

Stage 4 is planned around testing, peer feedback, release preparation, and the final demonstration. The scheduled milestones are Testing, Debugging & Polish on **25 November**, MVP Workshop and Peer Testing on **2 December**, Release Build and Poster Preparation on **9 December**, and the final demo and poster session on **16 December**.

## Team roles and rotation

Each team member has one position per stage. Product is shared by two co-leads to cover the five-person team. Stage 3 assigns each member a role not held by that person in Stage 1 or Stage 2.

| Team member | Stage 1 | Stage 2 | Stage 3 | Stage 4 |
|---|---|---|---|---|
| Reimo Zukker | Product Lead | Development Lead | Design Lead | Quality Lead |
| Raigo Leesment | Design Lead | Quality Lead | Product Co-Lead | To be assigned |
| Mairon Mihkelsoo | Quality Lead | Product Co-Lead | Development Lead | To be assigned |
| Robin Murumets | Development Co-Lead | Product Co-Lead | Quality Lead | To be assigned |
| Kermo Mätlik | Development Co-Lead | Design Lead | Product Co-Lead | To be assigned |

After Stage 3, Reimo will take the Quality Lead role in Stage 4. Other Stage 4 role assignments have not yet been set.

## Fit with the course project requirements

The course asks projects to combine two to four connected core feature areas, including data and persistence, UI and navigation, and API integration. It permits one or two additional features, including system integration or optional on-device AI/ML. Connect's core areas are cloud data and persistence, a multi-screen Compose interface, and Firebase-backed services. Camera and gallery sharing provide device integration. AI is not required for the current core scope and will not be claimed unless implemented and verified.

## Current limitations and next steps

Connect remains a course prototype rather than a public release. Direct chats load the latest 30 messages and do not yet paginate. Push notifications, uploaded profile photos, and end-to-end encryption are not implemented. The invite action opens Android's share sheet, but the app has no public distribution link. Group records cannot yet be edited or deleted from the client.

Next, the team will review authentication after Week 11, collect Stage 3 evidence for core flows and failure handling, and decide whether an AI feature is feasible after Week 12. Testing and peer feedback follow in Weeks 13–14. Contrast, touch targets, large system fonts, screen-reader use, and tablet/split-screen layouts also need further validation.

## References

- [Course project requirements](https://courses.cs.ut.ee/2026/MAD/fall/Main/ProjectRequirements)
- [Course page and schedule](https://courses.cs.ut.ee/2026/MAD/fall)
- Mobile Application Development lecture slides, Weeks 1–10, in the team's Google Drive Sources folder.
