---
name: ui
description: Modify the YoPt Compose Multiplatform UI — adjust layouts, add controls, restructure screens, or change composable parameters.
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

## Icons

All icons are `ImageVector` constants in `AppIcons.kt` using Material Symbols Rounded (`Icons.Rounded.*`). Never inline `Icons.Default.Xxx` or `Icons.Rounded.Xxx` in composables — always add a named constant to `AppIcons` and reference it.

```kotlin
// In AppIcons.kt — semantic name, not shape name
object AppIcons {
    val NewChat: ImageVector = Icons.Rounded.Add
    val AppendToPrompt: ImageVector = Icons.Rounded.Add  // same symbol, independent semantics
}

// In a composable
Icon(AppIcons.NewChat, contentDescription = "New chat", modifier = Modifier.size(18.dp))
```

Two constants can share the same underlying symbol — semantic names let them evolve independently.

## Tooltips

Every icon-only `TextButton` must be wrapped in `TooltipBox`. Pattern:

```kotlin
TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text("Do the thing") } },
    state = rememberTooltipState()
) {
    TextButton(onClick = { ... }) {
        Icon(AppIcons.SomeIcon, contentDescription = "Do the thing", modifier = Modifier.size(18.dp))
    }
}
```

Requirements:
- `@OptIn(ExperimentalMaterial3Api::class)` on the enclosing composable.
- Imports: `TooltipBox`, `PlainTooltip`, `TooltipDefaults`, `TooltipAnchorPosition`, `rememberTooltipState`, `ExperimentalMaterial3Api`.
- `contentDescription` on `Icon` matches the tooltip text intent.

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

## Resizable splitter

A draggable divider separates the top area (header + prompt + send) from the response list. Drag vertically to resize either area.

- **Persistence** — `SplitFractionRepository` (port) → `PlatformSplitFractionRepository` (expect/actual, platform-native prefs) → `ManageSplitFractionUseCase`. Default `0.4f` = `SplitFractionRepository.DEFAULT`. Written on drag end only via `scope.launch { splitFractionUseCase.set(value) }`.
- **Layout** — top area `Column(weight(splitFraction))`, divider `Box(12.dp)`, response `LazyColumn(weight(1f - splitFraction))`.
- **Clamp range** — `[0.2, 0.8]` prevents collapsing either area entirely.
- **Visual** — 12dp tall bar with `surfaceVariant` bg (50% alpha). Three 4dp dots centered as grip indicator. Highlights with `primary` at 10% alpha during drag.
- **Gesture** — `Modifier.pointerInput` with `detectVerticalDragGestures`. Pixel delta converted to fraction via `dragAmount / columnHeightPx` (outer Column height from `onGloballyPositioned`).
- **Prompt field** — uses `Modifier.weight(1f)` inside the top area to expand into available height. `maxLines = Int.MAX_VALUE`.

## MainScreen UI tree

```
Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(12.dp))
├── Column (top area — primaryContainer bg at 0.4 alpha, rounded 12.dp, 8.dp padding)
│   ├── BoxWithConstraints(Modifier.fillMaxWidth())
│   │   └── if (maxWidth < 630.dp) → narrow:
│   │       Column
│   │       ├── Row
│   │       │   ├── Box(weight 1f)
│   │       │   │   ├── OutlinedTextField(searchQuery, fillMaxWidth, trailing ChatListToggle icon)
│   │       │   │   └── DropdownMenu(filteredChats sorted by last activity desc)
│   │       │   └── Row — NewChat, DeleteChat (if >1 chat), ChatInstructions, Settings
│   │       └── OutlinedTextField(chatName, fillMaxWidth)  ← auto-saves per keystroke (debounced via coroutine)
│   │   └── else → wide:
│   │       Row
│   │       ├── Box(width = maxWidth * 0.3f)
│   │       │   ├── OutlinedTextField(searchQuery, fillMaxWidth, trailing ChatListToggle icon)
│   │       │   └── DropdownMenu(filteredChats sorted by last activity desc)
│   │       ├── OutlinedTextField(chatName, weight 1f)
│   │       └── Row — NewChat, DeleteChat (if >1 chat), ChatInstructions, Settings
│   ├── [Chat Settings Dialog] AlertDialog (when showChatSettings, currentChat != null)
│   │   ├── Column
│   │   │   ├── OutlinedTextField(instructions, 100dp, 5 maxLines)
│   │   │   └── Row(horizontalScroll) — tag chips (OutlinedButton 28.dp with "×" to remove) + "+ Add tag" button
│   │   ├── Button("Save") — persists instructions + labels via chatsUseCase.update()
│   │   └── TextButton("Cancel")
│   ├── [Add Tag Dialog] AlertDialog (nested inside Chat Settings, when showAddTagDialog)
│   │   └── OutlinedTextField(tag, singleLine) + Button("Add") + TextButton("Cancel")
│   ├── Spacer(8.dp)
│   ├── OutlinedTextField(prompt, weight(1f), maxLines=Int.MAX_VALUE)  ← expands to fill available height
│   ├── Spacer(8.dp)
│   ├── Row (model + action)
│   │   ├── Box(weight(1f)) + OutlinedButton(fillMaxWidth) + DropdownMenu  ← model selector; opens Settings if no models
│   │   ├── Spacer(8.dp)
│   │   ├── CircularProgressIndicator(24dp)  ← when loading (replaces Send button)
│   │   └── Button("Send")
│   ├── error? → Text(error, error color, bodySmall)
├── Splitter handle (12.dp, draggable, 3-dot grip, background highlights on drag)
│   ├── detectVerticalDragGestures — pixel delta → fraction delta (/ columnHeightPx), clamped [0.2, 0.8]
│   └── onDragEnd → onSetSplitFraction(splitFraction) — persists to KeyValueStore key "splitFraction"
└── LazyColumn(weight(1f - splitFraction), dot-grid background via drawBehind)
    └── itemsIndexed(history.reversed()) → OutlinedCard
        └── Column(4dp padding)
            ├── [if shown] Prompt area (surfaceVariant bg, rounded 8.dp, 630.dp BoxWithConstraints)
            │   Shown when: NOT first item, OR first item's prompt != current input, OR first item's model != selected model.
            │   Hidden when: first item AND prompt matches input AND model matches selection.
            │   ├── narrow OR promptExpanded → Column:
            │   │   ├── Row(End) — [if promptExpanded||promptOverflows] Expand/Collapse · UseAsPrompt · AppendToPrompt · CopyToClipboard
            │   │   └── Text(prompt, bodySmall, onSurfaceVariant, fillMaxWidth, maxLines=MAX/1, onTextLayout→promptOverflows)
            │   └── wide + collapsed → Row:
            │       ├── Text(prompt, bodySmall, onSurfaceVariant, weight 1f, maxLines=1, onTextLayout→promptOverflows)
            │       └── [if promptOverflows] Expand · UseAsPrompt · AppendToPrompt · CopyToClipboard
            │   Rule: UseAsPrompt/AppendToPrompt/CopyToClipboard always visible; Expand only when text overflows.
            ├── HorizontalDivider
            ├── Row (response actions, ALWAYS shown)
            │   Layout: Collapse/Expand LEFT-aligned; Raw/MD · UseAsPrompt · AppendToPrompt · CopyToClipboard RIGHT-aligned.
            │   Raw/MD button: only shown when respExpanded=true. Toggles entry.showMarkdown AND persists via responseDisplayUseCase.setDefaultShowMarkdown().
            │   Collapse/Expand icon: Expand when collapsed, Collapse when expanded.
            ├── [expanded] response content (selectable on capable platforms via SelectionContainer or BasicTextField)
            ├── [expanded && showRaw] BasicTextField (monospace, readOnly) — real text selection for Cmd+C
            ├── [collapsed] Text(response.take(200), monospace, maxLines=3)
            ├── Row (bottom info bar) — RemoveFromHistory · timestamp · " · modelName"(weight 1f fill false, ellipsis) · " · duration"
            └── [expanded && respContentHeightPx > windowHeightPx] Row (bottom repeat, Right-aligned) — same 5 buttons as top
```

## MainScreen state variables

| Variable | Type | Initial | Source |
|----------|------|---------|--------|
| `allChats` | `List<Chat>` | reactive | `chatsUseCase.observeAll().collectAsState(emptyList())` |
| `currentChatId` | `String?` | `null` | Local; set on init, dropdown select, new, delete |
| `currentChat` | `Chat?` | derived | `allChats.find { it.id == currentChatId }` |
| `chatName` | `String` | from `currentChat` | Title field; auto-saves via `chatsUseCase.update()` per keystroke; synced from currentChat when `currentChatId` changes |
| `chatSearchQuery` | `String` | `""` | Incremental filter for chat dropdown (matches title + labels) |
| `chatDropdownExpanded` | `Boolean` | `false` | Toggles chat search dropdown |
| `prompt` | `String` | from `lastPrompt` | Text field; saved on send, restored on launch. Keyed `remember(lastPrompt)` to sync when persisted value loads. |
| `lastPrompt` | `String` | reactive | `lastPromptUseCase.observe().collectAsState("")` — persisted via `lastPromptUseCase.set()` on send success |
| `lastChatId` | `String?` | reactive | `lastChatIdUseCase.observe().collectAsState(null)` — persisted via `lastChatIdUseCase.set()` on chat switch |
| `globalInstructions` | `String` | reactive | `globalInstructionsUseCase.observe().collectAsState("")` — persisted via `globalInstructionsUseCase.set()` on edit |
| `splitFraction` | `Float` | `SplitFractionRepository.DEFAULT` | Local mutable state synced from `splitFractionUseCase.observe().collectAsState(DEFAULT)`. Keyed `remember(persistedFraction)` to restore persisted value on startup. Controls height ratio between top area and response list. Clamped to `[0.2, 0.8]` during drag. Persisted via `splitFractionUseCase.set()` on drag end. |
| `columnHeightPx` | `Float` | `1f` | Outer Column height from `onGloballyPositioned` — used as denominator for pixel→fraction conversion in drag. |
| `loading` | `Boolean` | `false` | When true: show spinner, hide Send button |
| `error` | `String?` | `null` | Set on send failure; cleared (`= null`) at start of each new send |
| `showSettings` | `Boolean` | `false` | When true, renders `SettingsScreen` full-screen (replaces main UI) |
| `showChatSettings` | `Boolean` | `false` | Opens Chat Settings AlertDialog (instructions + tags) |
| `selectedModel` | `String?` | reactive | `modelSelectionUseCase.observe().collectAsState(null)` |
| `models` | `List<ModelDef>` | reactive | `modelsUseCase.observeEnabledModels().collectAsState(emptyList())` |
| `defaultShowMarkdown` | `Boolean` | reactive | `responseDisplayUseCase.observeDefaultShowMarkdown().collectAsState(false)` — global default for new responses |

Per-item local state (inside `itemsIndexed`):
- `respExpanded` — `Boolean`, starts `true` for first item OR word count < `RESPONSE_AUTO_EXPAND_WORD_LIMIT` (50). First item = index 0 (most recent after `history.reversed()`).
- `promptExpanded` — `Boolean`, starts `false`.
- `promptOverflows` — `Boolean`, from `onTextLayout.hasVisualOverflow`.
- `respContentHeightPx` — `Int`, from `onGloballyPositioned` on response content.
- `windowHeightPx` — `Int`, from `LocalWindowInfo.current.containerSize.height`.
- `rawFieldValue` — `TextFieldValue(entry.response)`, for selection-aware raw view.
- `showMarkdownCopyWarning` — `Boolean`, for macOS Cmd+C in markdown mode.

Three `LaunchedEffect` blocks:
1. Observes `allChats` + `lastChatId` — initializes `currentChatId`/`chatName` on first load (restores from `lastChatId` if present); handles deletion of current chat (switches to first remaining).
2. Syncs `chatName` when `currentChatId` changes (dropdown select, new chat). Persists `currentChatId` via `onSetLastChatId`.
3. Auto-selects first model if nothing selected or saved model no longer exists.

## Header button behaviors

| Button | Icon | Behavior |
|--------|------|----------|
| ChatListToggle | `AppIcons.ChatListToggle` | Toggles `chatDropdownExpanded` — opens/closes chat search dropdown |
| NewChat | `AppIcons.NewChat` | Creates chat via `chatsUseCase.create(title)`, sets `currentChatId = c.id` |
| DeleteChat | `AppIcons.DeleteChat` | Visible when `allChats.size > 1`. Deletes current chat immediately (no confirmation). Sets `currentChatId` to first remaining, calls `chatsUseCase.delete(chat.id)`. |
| ChatInstructions | `AppIcons.ChatInstructions` | Opens Chat Settings dialog (`showChatSettings = true`) |
| Settings | `AppIcons.Settings` | Opens full-screen Settings (`showSettings = true`) |

## Send flow

```
On Button click:
1. prompt.trim().isEmpty() → return
2. currentChat == null → return
3. scope.launch { loading = true; error = null
4.   if chatName changed: chatsUseCase.update(chat.copy(title = chatName))
5.   sendUseCase(chat.copy(title = chatName), prompt, selectedModel)
6.     .onSuccess → onSetLastPrompt(p)
7.     .onFailure → error = e.message
8.   loading = false
}
```

History persisted atomically by `SendPromptUseCase`. No local history state — `observeAll()` flow updates `currentChat.history` automatically.

Error cleared at line 4 (`error = null`). Error displays until next send attempt.

**Prompt persistence:** Last sent prompt saved via `scope.launch { lastPromptUseCase.set(p) }` on success. Prompt text field retains value after sending. Restored on launch via `lastPromptUseCase.observe().collectAsState("")`.

## Response display modes

Three modes per history entry, controlled by `respExpanded` (per-item) and `entry.showMarkdown` (per-entry):

| `respExpanded` | `entry.showMarkdown` | Renders |
|---|---|---|
| `false` | any | `Text(response.take(200), monospace, maxLines=3)` — truncated preview |
| `true` | `true` | `MarkdownResponse` (wraps multiplatform-markdown) inside `SelectionContainer` if platform supports text selection |
| `true` | `false` | `BasicTextField(value, readOnly=true, monospace)` — raw text with real selection for Cmd+C |

`showMarkdown` default for new entries comes from `responseDisplayUseCase.observeDefaultShowMarkdown()`. Toggling Raw/MD on any entry also persists the toggle as the new global default.

## macOS Cmd+C workaround

Compose 1.11.0 macOS native: `isCopyKeyEvent` unimplemented → crash on selection-capable widgets. Workaround via `needsCopyKeyInterceptor` expect/actual (true only on `macosArm64`):
- Intercepts `Key.C` keydown via `onPreviewKeyEvent`
- Raw view: copies real selection from `TextFieldValue.selection`
- Markdown view: `SelectionContainer` exposes no public selection API → shows `AlertDialog` explaining right-click Copy works

`supportsTextSelection` expect/actual: true on all platforms. Controls whether `SelectionContainer`/`BasicTextField` wrappers are used vs plain `Text`.

## Expand/collapse rules

**Response area** (above response content, always shown):
- Collapse/Expand button: LEFT-aligned in the Row. Icon: `AppIcons.Expand` when collapsed, `AppIcons.Collapse` when expanded.
- Raw/MD, UseAsPrompt, AppendToPrompt, CopyToClipboard: RIGHT-aligned in the same Row.
- Raw/MD button: only visible when `respExpanded = true`.

**Prompt area** (shown conditionally — see UI tree for hide rule):
- UseAsPrompt, AppendToPrompt, CopyToClipboard: always visible.
- Expand/Collapse: only when `promptExpanded || promptOverflows`.

**Bottom repeat:** When `respExpanded && respContentHeightPx > windowHeightPx`, same 5 buttons repeat at card bottom (all right-aligned).

**Auto-expand on load:**
- First history item (index 0, most recent): always expanded.
- Others: expanded if `wordCount < 50`, collapsed otherwise.

## SettingsScreen UI tree

Full-screen `Column` (replaces main UI when `showSettings = true`). Back button at top returns to main screen.

```
Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(12.dp))
├── Row (header)
│   ├── TooltipBox → TextButton(Back) → tooltip "Back", calls onDismiss
│   └── Text("Global Settings", titleLarge, weight 1f)
├── Spacer(8.dp)
├── TabRow(selectedTabIndex=tab)
│   ├── Tab("Providers")   ← tab 0
│   ├── Tab("Chats")       ← tab 1
│   ├── Tab("Global")      ← tab 2
│   └── Tab("Export")      ← tab 3
├── Spacer(8.dp)
└── when(tab)
    ├── 0 (Providers): LazyColumn(weight 1f)
    │   └── items(providers) → Card (expandable per provider)
    │       └── Column
    │           ├── Row: provider name + auth status (API key set / Logged in / Not configured)
    │           │         + [expanded] RefreshModels button (calls refreshModelsUseCase.refresh())
    │           │         + Collapse/Expand toggle
    │           └── [expanded] Column
    │               ├── ApiKeyAuth or OAuthAuth (auth section)
    │               └── [if provider has models] Column
    │                   ├── Row: filter TextField + "All off" + "All on" buttons
    │                   ├── Text("Models")
    │                   └── forEach model: Row(Checkbox(model.enabled) + Text(officialName))
    ├── 1 (Chats): Column(weight 1f)
    │   ├── [if any labels exist] Row(horizontalScroll) — label filter chips (Button=active, OutlinedButton=inactive)
    │   ├── OutlinedTextField(new chat title)
    │   ├── OutlinedTextField(new chat instructions, 60dp)
    │   ├── Button("+ Add Chat")
    │   └── LazyColumn
    │       └── items(filteredChats by checkedLabels) → Card
    │           ├── [View mode] Text(title), Row(labels as primary-colored text), Text(instructions.take(100)), Edit, Delete
    │           └── [Edit mode] TextField(title), TextField(instructions), Row(tag chips with × + "+ Add tag"), Save, Cancel
    │               └── [Add Tag Dialog] AlertDialog — TextField(tag) + Button("Add") + TextButton("Cancel")
    ├── 2 (Global): Column(weight 1f)
    │   └── OutlinedTextField(weight 1f) — global instructions, auto-saves per keystroke via onSetGlobalInstructions
    └── 3 (Export): Column
        ├── SaveFileButton — native save dialog (desktop), no-op on other platforms
        ├── OpenFileButton — native open dialog (desktop), no-op on other platforms
        ├── exportError → Text(error color)
        └── importError → Text(error color)
```

`SaveFileButton` and `OpenFileButton` are expect/actual composables — desktop uses AWT `FileDialog`, other platforms are no-ops. Export serializes all models, chats, and auth credentials; import deserializes and persists all three.

## SettingsScreen state variables

| Variable | Type | Initial | Notes |
|----------|------|---------|-------|
| `tab` | `Int` | `0` | Current tab index |
| `models` | `List<ModelDef>` | reactive | `manageModelsUseCase.observeModels().collectAsState()` |
| `chats` | `List<Chat>` | reactive | `manageChatsUseCase.observeAll().collectAsState()` |
| `creds` | `List<AuthCredentials>` | reactive | `manageAuthUseCase.observeCredentials().collectAsState()` |
| `newTitle` | `String` | `""` | New chat title field |
| `newInstr` | `String` | `""` | New chat instructions field |
| `globalInstructions` | `String` | reactive | From `globalInstructionsUseCase.observe().collectAsState("")`; auto-saves per keystroke via `onSetGlobalInstructions = { scope.launch { globalInstructionsUseCase.set(it) } }` |

Per-provider local state: `expanded` (Boolean), `filter` (String), `refreshing` (Boolean), `refreshError` (String?).
Per-chat edit state: `editing` (Boolean), `t` (String, title), `i` (String, instructions), `editingLabels` (List<String>), `showAddTagDialog` (Boolean), `newTagText` (String).

Model enabled state lives on `ModelDef.enabled`. Toggled via `manageModelsUseCase.setModelEnabled()`.

## Private composables (in SettingsDialog.kt)

### ApiKeyAuth

Three states:
- **Has key, not editing**: Text "API key is set" + TextButton("Change") + OutlinedButton("Clear")
- **Editing (or no key yet)**: OutlinedTextField + "Get API key ↗" link (opens provider URL) + Button("Save") + [if editing existing] TextButton("Cancel")

### OAuthAuth

Three states:
- **Not logged in**: Button("Login with {name}") — launches coroutine, stores `Job` for cancellation
- **Authorizing**: Spinner + "Authorizing in browser..." + instruction text + TextButton("Cancel") (cancels login Job)
- **Logged in**: Text("Logged in") + OutlinedButton("Logout")

## Tag chips pattern

Horizontally-scrollable `Row(horizontalScroll)` of `OutlinedButton` chips (28.dp height). Each chip: tag name + "×" suffix; click removes from list. "+ Add tag" button opens `AlertDialog` with `OutlinedTextField(singleLine)`. Add rejects empty and duplicate tags.

Tags persisted as `Chat.labels` via `chatsUseCase.update()`.

## Common operations

### Adding a new composable to MainScreen
1. Insert into the `Column` at the desired position (use the UI tree above).
2. Add `remember { mutableStateOf(...) }` for local session state.
3. For persisted state: add `observe/set` to the relevant repository, expose through use case, consume via `collectAsState()`.
4. Thread new use cases through `MainScreen` constructor and `App()` call in App.kt.

### Adding a new tab to SettingsScreen
1. Add `Tab` in `TabRow`.
2. Add `when(tab)` branch.
3. Add state variables at the top of the dialog.

### Adding a new provider auth type
1. If new `AuthType` variant: add to sealed class.
2. Add `when` branch in the expanded provider card (Providers tab).
3. Wire through `ManageAuthUseCase`.

### Changing spacing/padding
- Every full-screen `Column` root: `Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(12.dp)`.
- `Spacer(Modifier.height(n.dp))` between sections.
- `FlowRow` uses `horizontalArrangement = Arrangement.spacedBy(8.dp)`.

### Changing component sizes
- Prompt input: `Modifier.weight(1f)` fills available height in the top area; `maxLines = Int.MAX_VALUE` allows unlimited text lines.
- History preview (collapsed): `response.take(200)`, `maxLines=3`.
- Chat instructions in settings: `.height(60.dp)`.

### Changing colors
Edit `LightColors` / `DarkColors` in `Theme.kt`. No color literals in composables — always use `MaterialTheme.colorScheme.X`.
