# YoPt — KMP AI Chat App

## Project Overview

**Stack:** Kotlin 2.3.21 · Compose Multiplatform 1.11.0 · AGP 9.2.1 · Ktor · SKIE ·
`kotlinx.serialization`

**Modules:**

- `:shared` — domain models, ports (interfaces), use cases, infra adapters (`s4y.yopt.infra`)
- `:composeApp` — Compose UI for all platforms (`s4y.yopt.ui`)
- `:androidApp` — thin Android app shell (`MainActivity` delegates to `composeApp`)

**KMP targets** (`:shared`): `android` · `desktop` (JVM) · `iosArm64` · `iosSimulatorArm64` · `macosArm64` · `macosX64` · `wasmJs`
**KMP targets** (`:composeApp`): `android` · `desktop` (JVM) · `iosArm64` · `iosSimulatorArm64` · `macosArm64` (framework only, for xcodeApp) · `wasmJs`

## Running

| Platform      | Command                                  |
|---------------|------------------------------------------|
| Desktop (JVM) | `./gradlew :composeApp:run` ✅            |
| Web (wasmJs)  | `./gradlew :composeApp:wasmJsBrowserRun` |
| Android       | Android Studio → run `:androidApp`       |
| iOS / macOS   | Xcode → `xcodeApp/`                      |

## Architecture

Hexagonal / clean. Dependencies point inward: **UI → usecases → domain.services → domain.ports ← adapters**.

Full rules (port rule, service rule, use-case rule, persistence decision tree, reactive
pattern) are in `.claude/skills/architecture.md` — read it before changing structure.

```
shared/src/commonMain/kotlin/s4y/yopt/
  domain/models/    — data classes + enums
  domain/ports/     — boundary interfaces (one per boundary, even single-impl)
  domain/services/  — domain logic as plain classes
  usecases/         — UI-facing orchestration
  adapters/         — port implementations
  AppModule.kt      — manual DI wiring

composeApp/src/commonMain/kotlin/s4y/yopt/
  App.kt          — root composable, creates AppModule
  ui/             — screens (MainScreen, SettingsDialog)
  ui/AppIcons.kt  — all icon constants
```

**DI:** manual `AppModule` (no framework). Instantiated in each platform entry point.

**Entry points:**

- Desktop: `composeApp/src/desktopMain/…/Main.kt` (`fun main()`)
- Android: `androidApp/src/main/…/MainActivity.kt`
- macOS native: `composeApp/src/macosArm64Main/…` (entry `s4y.yopt.main`)

## LLM Providers

`PredefinedProviders.kt` — Anthropic, OpenAI, Google Gemini, OpenRouter, DeepSeek, Qwen

API styles: `ApiStyle.ANTHROPIC` / `ApiStyle.OPENAI` / `ApiStyle.GEMINI`

Auth: `ApiKey` only (OAuth removed). Credentials stored in `SecureStore`.

## Rules

### UI (Compose — composeApp)

- **i18n required** — all user-visible strings must use `stringResource(Res.string.xxx)`. Never
  hardcode English strings in composables. Add new strings to `values/strings.xml`.
- **No ViewModel** — state via `remember { mutableStateOf(...) }` directly in composables.
- **Reactive persisted state** — use cases expose `Flow`; UI subscribes via `collectAsState()`.
  Never use `LaunchedEffect` to load initial values.
- **Material 3** — use `androidx.compose.material3.*` components only.
- **Icons** — always define as semantic named `ImageVector` constants in `AppIcons.kt` using
  `Icons.Rounded.*`. Never inline `Icons.Rounded.Xxx` or `Icons.Default.Xxx` in composables.
- **Tooltips** — every icon-only button must be wrapped in `TooltipBox`. Enclosing composable
  requires `@OptIn(ExperimentalMaterial3Api::class)`.

### UI (SwiftUI — xcodeApp)

- **Visual parity** — SwiftUI must match Compose visually. When changing Compose layouts, colors,
  or icons, update the SwiftUI counterparts. Design constants live in `xcodeApp/Shared/DesignTokens.swift`.
- **DesignTokens** — never hardcode colors, radii, padding, or icon sizes in SwiftUI views.
  Always use `DesignTokens.*` constants.
- **Model selection** — call `viewModel.selectModel(_ id:)` to persist selection via Kotlin bridge.
  Never set `viewModel.selectedModel` directly.
- **Expand logic** — write `(chat?.expandedTimestamps.contains(ts) ?? false) || isFirst || wordCount < 50`.
  Parenthesise the `??` operand — Swift's `??` binds differently than Kotlin's `?:`.
- **New Swift files** — must be registered in `xcodeApp/YoPt.xcodeproj/project.pbxproj` with a
  `PBXFileReference`, group entry, and two `PBXBuildFile` + `PBXSourcesBuildPhase` entries (iOS
  target `B2` + macOS target `B4`). See `.claude/skills/ui.md` for the pattern.
- **SF Symbol → AppIcons mapping** — see `.claude/skills/ui.md` for the full table.

### Domain

- **Model enabled state** — stored on `ModelDef.enabled` and toggled via the model service's `setModelEnabled()`. No separate disabled-models store.
- **UI never touches services/ports directly** — all mutations go through use case methods.
  No `chat.copy(history = ...)` in composables; add a use case method that encapsulates the
  operation and delegates to the service.
- For layering, the port/service/use-case rules and the persistence decision tree, see
  `.claude/skills/architecture.md`.

### Infrastructure

- **Package** — all adapter implementations in `s4y.yopt.adapters` (not `internal`).
- **Never mix observable and getter** — if data has `observe*(): Flow<T>`, do not add a `get*(): T`
  for the same data. The flow is the single source of truth (one-shot data with no flow excepted).

## Available Skills

- `.claude/skills/architecture.md` — layering rules, ports vs services, use-case rule, persistence decision tree, reactive pattern, DI
- `.claude/skills/ui.md` — modify Compose or SwiftUI layouts, add controls, keep visual parity; includes SwiftUI file map, DesignTokens reference, icon mapping table, xcodeproj registration pattern
- `.claude/skills/llm.md` — LLM integration layer (API styles, adding providers, model fetching)
- `.claude/skills/icons.md` — add Material Symbols icons from gstatic URLs to AppIcons.kt
- `.claude/skills/kmp-xcode.md` — iOS / Xcode KMP integration
