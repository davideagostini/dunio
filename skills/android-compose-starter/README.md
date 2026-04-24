# android-compose-starter

A skill that acts as an Android + Jetpack Compose expert. It guides you through scaffolding a production-ready Android project from scratch and enforces modern architecture patterns, library choices, and Compose best practices throughout development.

---

## What it covers

The skill ships with 16 reference files:

| Reference                  | Topics                                                                                                            |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `project-setup.md`         | Android Studio setup, Gradle KTS, version catalog, `gradle.properties`, release config                            |
| `architecture.md`          | Feature-first MVVM, Hilt DI, `BaseViewModel`, `StateFlow`, Navigation Compose, Repository pattern                 |
| `libraries.md`             | Library selection guide by category, how to find latest stable versions, full `libs.versions.toml` template       |
| `state-management.md`      | `remember`, `rememberSaveable`, `derivedStateOf`, `snapshotFlow`, `@Stable`/`@Immutable`                          |
| `side-effects.md`          | `LaunchedEffect`, `DisposableEffect`, `SideEffect`, `rememberCoroutineScope`, `rememberUpdatedState`              |
| `view-composition.md`      | Composable naming, slot pattern, stateless/stateful split, `CompositionLocal`, screen-level patterns              |
| `composition-locals.md`    | Custom locals, `staticCompositionLocalOf` vs `compositionLocalOf`, framework-provided locals, testing overrides   |
| `lists-and-scrolling.md`   | `LazyColumn`, `LazyGrid`, `HorizontalPager`, keys, `contentType`, scroll state, Paging 3                          |
| `performance.md`           | Recomposition skipping, deferred state reads, `LazyColumn` keys, `contentType`, Baseline Profiles                 |
| `animation.md`             | `animate*AsState`, `AnimatedVisibility`, `AnimatedContent`, `Crossfade`, `updateTransition`, `graphicsLayer`      |
| `accessibility.md`         | Semantics, `contentDescription`, touch targets (48dp), headings, live regions, traversal order                    |
| `modifiers.md`             | Modifier ordering, `graphicsLayer`, `Modifier.Node`, `clickable`, custom modifiers                                |
| `deprecated-migrations.md` | 19 deprecated-to-current migrations: accompanist replacements, KAPT→KSP, Material2→3, type-safe nav, edge-to-edge |
| `principles.md`            | Core architectural guardrails and the most important implementation rules                                           |
| `common-bugs.md`           | High-frequency Compose and ViewModel mistakes to avoid                                                             |
| `readiness-checklist.md`   | Final project, architecture, code quality, and release checks                                                      |

---

## Typical use cases

- Scaffold a new Android app with Jetpack Compose
- Set up a new app module with consistent architecture and dependency choices
- Add the first feature screen to a greenfield project
- Get Compose-specific guidance grounded in a documented workflow

---

## How it works

The skill defines a 6-step workflow:

1. **Create the project** — Android Studio setup, Gradle KTS, version catalog
2. **Select libraries** — category-by-category, latest stable versions only
3. **Architecture skeleton** — Hilt, `BaseViewModel`, Navigation, DI modules
4. **First feature screen** — `State`, `Event`, `ViewModel`, `Composable`, route wiring
5. **Compose best practices** — the agent consults the relevant reference file for each concern
6. **Release checklist** — R8/ProGuard, signing, edge-to-edge

At each step the agent consults the matching reference file before giving instructions, ensuring advice is grounded in documented patterns rather than improvised.

---

## Expected structure

```text
android-compose-starter/
├── SKILL.md
├── README.md
└── references/
    ├── accessibility.md
    ├── animation.md
    ├── architecture.md
    ├── common-bugs.md
    ├── composition-locals.md
    ├── deprecated-migrations.md
    ├── libraries.md
    ├── lists-and-scrolling.md
    ├── modifiers.md
    ├── performance.md
    ├── principles.md
    ├── project-setup.md
    ├── readiness-checklist.md
    ├── side-effects.md
    ├── state-management.md
    └── view-composition.md
```

---

## Example prompts

```text
/android-compose-starter MyTaskApp — a simple task manager with list, detail, and settings screens
```

```text
/android-compose-starter add a paginated task list screen with search and pull-to-refresh
```

---

## Authoring rules

- **Version catalog is mandatory** — no inline version strings in `build.gradle.kts`
- **Prefer KSP over KAPT** where supported
- **Use `collectAsStateWithLifecycle()`** instead of `collectAsState()`
- **Keep business logic out of composables**
- **Consult the relevant Compose reference before giving detailed implementation advice**

Version numbers shown in examples are illustrative. Always refresh them against the latest stable releases before applying them to a real project.
