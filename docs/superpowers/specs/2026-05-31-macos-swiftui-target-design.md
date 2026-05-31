# Native SwiftUI macOS Target — Design

**Date:** 2026-05-31
**Status:** Proposed (awaiting review)
**Author:** brainstorming session

## Problem

The macOS **native** target (`macosArm64`, Kotlin/Native + Compose Multiplatform 1.11.0)
has a fundamentally broken text stack. Confirmed by running the app:

- **Cmd+C copy** silently no-ops in every editable input (prompt box, search, all
  Settings fields) and crashes outright in the rendered-markdown response view
  (`SelectionContainer`).
- **Meta-key handling** is incorrect in input fields.
- **Non-latin character input** is blocked — text input is effectively unusable for
  non-ASCII users.

Root cause is in CMP's macOS-native key-event classification (`isCopyKeyEvent` and
related) being unimplemented. This is not patchable per-field without reimplementing
copy/paste/cut/select-all/IME by hand for every input — untenable.

CMP 1.11.0 is the latest release and ships **zero** macOS features (it even removed the
`macosX64` target); there is no public AppKit/`NSView` interop (no macOS equivalent of
iOS `UIKitView`), so embedding a native `NSTextView` through Compose is not possible with
public APIs. See prior investigation in `TODO.md` (lines 9-118) and the CMP 1.11.0
release notes.

The Compose **Desktop (JVM)** target was considered as an escape hatch (its AWT text
backend works) but rejected by the product owner.

## Decision

Build a **native SwiftUI application for the macOS target only**. Keep Compose
Multiplatform for iOS, Android, Desktop (JVM), and Web. Reuse the existing `:shared`
Kotlin Multiplatform module unchanged via the `YoPtShared` framework.

This is the standard architecture for a KMP app that needs first-class native macOS text
behaviour: shared business logic in Kotlin, native UI in SwiftUI. AppKit gives correct
copy/paste/cut/select-all, meta keys, and IME/Unicode input for free.

### Non-goals

- **iOS stays on Compose.** iOS text input and clipboard work correctly today; no rewrite.
- **No changes to Android / Desktop / Web UI.**
- **No partial drag-selection across rendered markdown.** Copy is button-driven (see
  Markdown & Copy), matching every major LLM chat app. This was explicitly accepted.
- **No new product features.** This is a UI re-platform of the existing macOS experience
  at feature parity.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Apple platforms                                             │
│                                                             │
│  iOS  ── Compose UI (composeApp, ComposeApp.framework)      │
│  macOS ── SwiftUI  (xcodeApp/macApp)  ◄── NEW               │
│                    │                                        │
│                    └── binds to ──┐                         │
│                                   ▼                         │
│         :shared  ──►  YoPtShared.framework (SKIE)           │
│         domain / usecases / adapters / AppModule           │
└─────────────────────────────────────────────────────────────┘
```

- The SwiftUI macOS app depends **only** on `YoPtShared` (the `:shared` framework). It
  does **not** link `ComposeApp` and does not call `MainViewController`.
- `AppModule` is instantiated from Swift (`AppModule(platformContext:)`). All UI state is
  derived from the 13 use cases it exposes.
- **SKIE** (already configured, `co.touchlab.skie` 0.10.12) bridges Kotlin `Flow` to
  Swift `AsyncSequence` and sealed classes to Swift enums, making reactive binding
  ergonomic. The SwiftUI layer wraps each consumed `Flow` in an `@Observable`/
  `ObservableObject` view-model that collects the flow into `@Published` state. Mutations
  call use-case methods (`Task { await useCase.set(...) }`).

### Use-case surface (the binding contract)

The SwiftUI app consumes exactly the use cases `AppModule` exposes (see
`shared/.../AppModule.kt`):

| Use case | Drives |
|----------|--------|
| `SendPromptUseCase` | send prompt, receive response, streaming/duration |
| `ManageModelsUseCase` | model list, enable/disable |
| `ManageModelSelectionUseCase` | selected model |
| `ManageChatsUseCase` | chat list, create/delete/rename/tags, history |
| `ExportImportUseCase` | settings export/import |
| `ManageAuthUseCase` | API-key credentials per provider |
| `RefreshModelsUseCase` | fetch models from provider |
| `ManageProvidersUseCase` | provider CRUD, predefined providers |
| `ManageResponseDisplayUseCase` | raw/markdown + expand state |
| `ManageLastChatIdUseCase` | restore last chat |
| `ManageLastPromptUseCase` | restore draft prompt |
| `ManageSplitFractionUseCase` | split-pane fraction |
| `ManageGlobalInstructionsUseCase` | global system instructions |

No use-case signatures change. If any expose a getter that is awkward from Swift, that is
handled in the implementation plan, not here. (Note: two getter-vs-flow cleanups are
already tracked in `TODO.md` "Known Problems" and are out of scope for this re-platform.)

## Screen inventory to port

Port at **feature parity** with the current Compose UI.

### Main screen (`MainScreen.kt`)
- **Header**: model/chat search; chat name; action buttons (new chat, settings, export,
  etc.). Responsive layout (wide vs narrow) — SwiftUI adaptive layout.
- **Prompt input**: multiline text editor; Cmd+Enter / Ctrl+Enter to send; placeholder.
  Native `TextEditor` — selection, paste, IME, undo all free.
- **History list**: per-item prompt area + response area.
  - Prompt: collapsed (1 line) / expanded; Replace / Append / Copy actions.
  - Response: Raw / Markdown toggle; Collapse/Expand; UseAsPrompt / Append / Copy;
    auto-expand rule (first item, or word count < 50); bottom action row when tall.
  - Bottom bar: remove-from-history, timestamp, model name, duration.
- **Split pane** between input and history, fraction persisted via
  `ManageSplitFractionUseCase`. Native `NSSplitView` (via `NSSplitViewController` /
  SwiftUI split) — draggable.

### Settings (`SettingsDialog.kt`) — 4 tabs
1. **Providers** — provider list, add/edit/delete; per provider: name, base URL, API
   style picker, API key field, refresh-models, model enable/disable list with filter +
   All-on/All-off.
2. **Chats** — chat management.
3. **Global** — global instructions.
4. **Export** — export/import settings.

All inputs are native SwiftUI controls (`TextField`, `SecureField` for API keys,
`Picker`, `Toggle`, `List`).

## Markdown & copy

Markdown rendering on Apple platforms is a solved, routine problem; the only hard part is
free drag-selection across a rich document, which we explicitly do not require.

- **Renderer: `swift-markdown-ui`** (gonzalezreal). Community standard, actively
  maintained, renders headings, lists, tables, fenced code blocks, blockquotes; themeable
  to match the app. Added via Swift Package Manager.
- **Copy is button-driven**, exactly like ChatGPT / Claude / Perplexity:
  - whole-message copy button (already backed by existing copy actions / clipboard),
  - per-code-block copy button (supported by swift-markdown-ui),
  - native `.textSelection(.enabled)` for within-block selection.
- **Raw mode**: native `Text` / `TextEditor`(read-only) — full native selection + Cmd+C.
- The swift-markdown-ui cross-paragraph selection limitation (issue #264) does **not**
  matter because copy is button-driven.
- **Escape hatch (not the plan):** if guaranteed full-document selection is ever required,
  render via Apple's official `swift-markdown` parser → `NSAttributedString` →
  selectable `NSTextView`. Recorded for completeness; not implemented now.

## Internationalisation

User-visible strings currently live in Compose resources (`values/strings.xml`,
`Res.string.*`) and are not readable from SwiftUI. The app is English-only (B1/B2 per
project rules).

**Decision:** the SwiftUI macOS app uses its own `Localizable.strings` (English),
populated from the existing `strings.xml` values. This duplicates strings for the macOS
target only. Acceptable given English-only scope; if multi-locale is ever needed, promote
the canonical strings into `:shared` and expose them. (Recorded as a known trade-off.)

## Persistence & security

Unchanged — both reused from `:shared`:
- `KeyValueStore` (preferences/chats/models) — existing macOS actual.
- `SecureStore` (API keys) — existing Apple/macOS actual (Keychain).

No new persistence is introduced in the SwiftUI layer; all reads/writes go through use
cases.

## Build & Xcode wiring

- **`:shared`** keeps its `macosArm64` framework (`YoPtShared`, static, SKIE). This is the
  dependency the SwiftUI app links.
- **`:composeApp`**: drop the `macosArm64` native target and the related Gradle packaging
  (`packageMacosApp`, `runMacosNative`, `runDebugExecutableMacosArm64` workarounds). The
  Compose macOS native binary is no longer produced or shipped.
- **`xcodeApp/macApp`**: replace the Compose host (`ContentView` →
  `MainViewControllerKt.MainViewController()`, and `MacOsAppKt.createAppWindow()`) with a
  native SwiftUI `App` + scenes. Existing `macApp/YoPt.entitlements`, `Info.plist`,
  `Assets.xcassets` (icons) are reused. Window frame save/restore already in
  `macOSApp.swift` is kept/adapted.
- Add `swift-markdown-ui` via SwiftPM to the macApp target.
- Build the `YoPtShared` framework from Gradle as an Xcode build phase / linked framework
  (same mechanism iOS already uses).

## Distribution

macOS app ships as a standard `.app`/`.dmg` built and signed through Xcode
(notarizable). Entitlements already exist. This replaces the Gradle `packageMacosApp`
output.

## Risks

- **Dual UI maintenance**: macOS SwiftUI + Compose elsewhere. Every macOS-facing UI change
  is done twice. Accepted as the cost of a working native text stack.
- **swift-markdown-ui parity**: must match current markdown rendering (tables, code
  blocks). Mitigation: it supports all required blocks; theme to match.
- **Binary size** grows (SwiftUI + K/N framework, ~larger than the native kexe). Accepted.
- **Reactive glue**: SKIE Flow→Swift bridging must be wired correctly for every observable
  use case. Mitigation: one reusable `@Observable` flow-collector wrapper.
- **Feature drift**: parity gaps during port. Mitigation: the screen inventory above is the
  acceptance checklist.

## Verification

- Manual on macOS: Cmd+C/Cmd+V/Cmd+X/Cmd+A in every input; non-latin (e.g. Cyrillic) input
  via keyboard + IME; markdown rendering of a response containing headings, lists, a table,
  and a fenced code block; per-message and per-code-block copy; raw-mode selection + copy;
  split-pane drag persistence; chat create/delete/rename; provider add/edit/delete; API-key
  save (Keychain) and model refresh; export/import; window frame restore.
- Parity check against the Compose build for each item in the screen inventory.
- Shared Kotlin logic is unchanged, so existing `:shared` tests continue to cover domain.

## Rollout

1. Stand up the SwiftUI macApp shell linking `YoPtShared`; instantiate `AppModule`.
2. Build the reactive flow-collector wrapper + view-models over the 13 use cases.
3. Port Main screen (header → prompt input → history → split pane).
4. Port Settings (4 tabs).
5. Wire markdown (swift-markdown-ui) + copy buttons.
6. Localizable.strings from strings.xml.
7. Remove `:composeApp` macOS native target + Gradle packaging.
8. Verify against the checklist; package/sign `.app`.
