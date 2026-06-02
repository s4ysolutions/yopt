---
name: ui
description: Modify the YoPt UI — Compose Multiplatform (Android/Desktop/iOS/macOS/Web) or SwiftUI (xcodeApp iOS/macOS). Adjust layouts, add controls, restructure screens, or keep visual parity between the two UIs.
---

# YoPt UI Skill

Kotlin Multiplatform Compose UI for an LLM chat client. Targets Android, Desktop (JVM), macOS native, iOS, and WASM/JS.

## Source files

| File | Path | Purpose |
|------|------|---------|
| MainScreen | `composeApp/src/commonMain/kotlin/s4y/yopt/ui/MainScreen.kt` | Primary chat interface |
| MarkdownResponse | `composeApp/src/commonMain/kotlin/s4y/yopt/ui/MarkdownResponse.kt` | Wraps `com.mikepenz.markdown.m3.Markdown` with custom table styling (header/row: `maxLines=Int.MAX_VALUE`, `overflow=Clip`). Single composable — not expect/actual. |
| SettingsDialog | `composeApp/src/commonMain/kotlin/s4y/yopt/ui/SettingsDialog.kt` | Settings screen (4 tabs); also contains `ApiKeyAuth` and `OAuthAuth` (see Private composables below) |
| AppIcons | `composeApp/src/commonMain/kotlin/s4y/yopt/ui/AppIcons.kt` | All icon constants — `ImageVector` using Material Symbols Rounded |
| Theme | `composeApp/src/commonMain/kotlin/s4y/yopt/ui/Theme.kt` | `YoPtTheme` — custom `lightColorScheme` / `darkColorScheme` + `Typography`. Wraps `MaterialTheme`. |
| App | `composeApp/src/commonMain/kotlin/s4y/yopt/App.kt` | Root composable, creates `AppModule`, wraps content in `YoPtTheme` |
| SelectionCapability | `composeApp/src/commonMain/.../ui/SelectionCapability.kt` | `expect val supportsTextSelection: Boolean` / `expect val needsCopyKeyInterceptor: Boolean` — platform `actual` in each source set |

## Architecture

- **No ViewModel** — state is `remember { mutableStateOf(...) }` in composables.
- **Reactive persisted state** — use cases expose `Flow`; UI reads via `collectAsState()`. Never `LaunchedEffect` to load initial values. The flow IS the initial load.
- **DI** — `MainScreen` receives use cases from `App()`. Thread new use cases through the constructor and `App()` call site.
- **Persistence** — simple preferences use port/repo/usecase pattern: `SplitFractionRepository` (port) → `PlatformSplitFractionRepository` (expect/actual infra) → `ManageSplitFractionUseCase`. Default value (`0.4f`) defined as `SplitFractionRepository.DEFAULT` domain constant.
- **Material 3** only — `androidx.compose.material3.*`.
- **Coroutines** — `rememberCoroutineScope()` + `scope.launch` for all async work.
- **Safe area** — every full-screen `Column` root must use `Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(12.dp)`. No-op on desktop; keeps content clear of status/nav bars on Android/iOS.



## Theming

Custom theme in `Theme.kt` — `YoPtTheme` composable wraps `MaterialTheme` with:
- `LightColors` / `DarkColors` — custom `lightColorScheme`/`darkColorScheme` (primary blue `#1A56DB`, secondary teal `#0D9488`, tertiary violet `#7C3AED`)
- `YoPtTypography` — custom `Typography` with SemiBold titles, Normal body, Medium labels

Use `MaterialTheme.colorScheme.X` and `MaterialTheme.typography.Y` in composables. To change colors app-wide, edit `Theme.kt`.

## Responsive layout

`BoxWithConstraints` with 630.dp threshold toggles between narrow/wide layouts:
- **Wide** (`maxWidth >= 630.dp`): single `Row` — search `Box(width = maxWidth * 0.3f)` | chat name `weight(1f)` | icon button Row
- **Narrow** (`maxWidth < 630.dp`): two-row `Column` — top Row: search `Box(weight(1f))` + icon button Row; second Row: chat name `fillMaxWidth`

Capture `maxWidth` as a local `val` before nested lambdas to avoid implicit-receiver errors.

`FlowRow` used in bottom info bars for independent wrapping. Requires `@OptIn(ExperimentalLayoutApi::class)`.

Long text overflow: model selector uses `Box(weight(1f))` + `OutlinedButton(fillMaxWidth)` with `Text(maxLines=1, overflow=Ellipsis)`. Model name in bottom bar: `Text(maxLines=1, overflow=Ellipsis, modifier=weight(1f, fill=false))`.



## ApiKeyAuth

Three states:
- **Has key, not editing**: Text "API key is set" + TextButton("Change") + OutlinedButton("Clear")
- **Editing (or no key yet)**: OutlinedTextField + "Get API key ↗" link (opens provider URL) + Button("Save") + [if editing existing] TextButton("Cancel")

# SwiftUI (xcodeApp)

Parallel native app for iOS and macOS sharing the same Kotlin domain via `ComposeApp.xcframework`.
Compose UI is the source of truth — SwiftUI should visually match it. When changing layouts/colors/icons in Compose, update the SwiftUI counterparts too.

## DesignTokens

`Shared/DesignTokens.swift` Always use these — never hardcode values.

| Token | Value | Compose equivalent |
|---|---|---|
| `topAreaBackground` | `accentColor.opacity(0.08)` | `primaryContainer.copy(alpha=0.4f)` |
| `topAreaCornerRadius` | `12` | `RoundedCornerShape(12.dp)` |
| `cardCornerRadius` | `8` | `RoundedCornerShape(8.dp)` |
| `sectionPadding` | `8` | `8.dp` inner padding |
| `cardVerticalPadding` | `4` | `padding(vertical=4.dp)` on cards |
| `iconSize` | `18` | `Modifier.size(18.dp)` |
| `actionBarHeight` | `24` | action bar frame height |
| `dotSpacing` / `dotRadius` / `dotOpacity` | `20` / `2` / `0.12` | dot-grid `drawBehind` params |

`dotGridBackground()` view modifier draws the response-area dot grid (matches Compose's `drawBehind` circle pattern).

## SwiftUI cross-platform safety (`Shared/` = iOS + macOS)

Files in `Shared/` compile for both platforms. Platform APIs need `#if` guards:

```swift
#if os(macOS)
    Color(nsColor: .controlBackgroundColor)
#else
    Color(uiColor: .systemBackground)
#endif
```

Never use `Color(uiColor:)` / `UIColor` / `UIImage` in `Shared/` without `#if os(iOS)`. Never use `Color(nsColor:)` / `NSColor` in `Shared/` without `#if os(macOS)`. Prefer `DesignTokens.*` — handles platform branching internally.

## SwiftUI API compatibility

`onChange(of:perform:)` deprecated macOS 14+. Use:
```swift
.onChange(of: value) { newValue in ... }  // ✓
.onChange(of: value) { ... }              // ✓ zero-param
```


