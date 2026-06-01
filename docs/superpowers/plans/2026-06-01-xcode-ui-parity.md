# xcodeApp UI Parity with ComposeUI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make xcodeApp SwiftUI layouts and colors match ComposeUI visually. Apple HIG takes priority over Compose conventions where they conflict.

**Architecture:** Four independent fixes across four files. No new files. No new types.

**Tech Stack:** SwiftUI, macOS. `DesignTokens.swift` for constants; `MacMainChatView.swift` owns top-level layout.

---

## Full Diff Analysis: xcode vs Compose

### CRITICAL (layout broken or major visual mismatch)

| # | Area | Compose | xcode (current) | Impact |
|---|---|---|---|---|
| A | **Settings window size** | Full-screen replacement: `if showSettings { SettingsScreen() }` replaces entire window | `.sheet` modal `frame(minWidth:500, minHeight:400)` | Settings is a tiny floating panel; all tab content is cramped |
| B | **ChatsTabView scroll** | `LazyColumn(Modifier.weight(1f))` — fills remaining height, scrollable | `LazyVStack` inside bare `VStack` inside `Group` — no `ScrollView` | With many chats, content overflows and clips; not scrollable |
| C | **GlobalTabView height** | `OutlinedTextField(Modifier.weight(1f))` — fills all remaining height | Bare `TextEditor` with no frame constraint | TextEditor collapses to intrinsic minimum; unusable for multi-line global instructions |

### Minor (visual nuance, Apple HIG where relevant)

| # | Area | Compose | xcode | Action |
|---|---|---|---|---|
| D | Top area background tint | `primaryContainer.copy(alpha=0.4f)` — clearly visible | `accentColor.opacity(0.08)` — barely visible | Raise to `0.4` |
| E | Card corner radius | `OutlinedCard` = 12dp default | `cardCornerRadius = 8` | Raise to `12` |
| F | Provider status color | `primary` (accent blue) when key set | `.green` when key set | Use `Color.accentColor` |
| G | Bottom bar model label | `weight(1f, fill=false)` + ellipsis | No width limit; `Spacer()` at end | Constrain with `maxWidth: .infinity` + `lineLimit(1)` |

### Intentionally kept different (Apple HIG priority)

- `Picker(.segmented)` for settings tabs (vs Material `PrimaryTabRow`) — Apple standard
- `Toggle` for model enable/disable (vs `Checkbox`) — Apple standard
- `TextEditor` with manual border (vs `OutlinedTextField`) — native on Apple
- `ChatSettingsView` as floating overlay card (vs `AlertDialog`) — Apple standard
- SF Symbol chevrons for provider expand/collapse — Apple standard

---

## File Map

| File | Change |
|---|---|
| `xcodeApp/macApp/Views/MacMainChatView.swift` | Replace `.sheet` for settings with inline conditional (full-screen) |
| `xcodeApp/Shared/Components/SettingsView.swift` | Add `.frame(maxWidth:.infinity, maxHeight:.infinity)` |
| `xcodeApp/Shared/Components/ChatsTabView.swift` | Wrap `LazyVStack` in `ScrollView` |
| `xcodeApp/Shared/Components/GlobalTabView.swift` | Give `TextEditor` `.frame(maxHeight:.infinity)` |
| `xcodeApp/Shared/DesignTokens.swift` | `topAreaBackground` 0.08→0.4; `cardCornerRadius` 8→12 |
| `xcodeApp/Shared/Components/ProvidersTabView.swift` | Provider status `.green` → `Color.accentColor` |
| `xcodeApp/Shared/Components/ResponseCardView.swift` | Bottom bar model label layout |

---

## Task 1: Settings — full-screen replacement instead of sheet

**Files:**
- Modify: `xcodeApp/macApp/Views/MacMainChatView.swift:117-119`
- Modify: `xcodeApp/Shared/Components/SettingsView.swift:49`

Compose replaces the entire window content when settings is open. The xcode `.sheet` produces a cramped 500×400 modal that makes all settings tabs feel broken regardless of their own layout.

- [ ] **Step 1: Change MacMainChatView to show SettingsView inline**

  In `xcodeApp/macApp/Views/MacMainChatView.swift`, replace the `.sheet` modifier and the entire `body` structure so that when `showSettings` is true, only `SettingsView` is shown (full area):

  ```swift
  var body: some View {
      Group {
          if viewModel.showSettings {
              SettingsView(viewModel: viewModel)
                  .frame(maxWidth: .infinity, maxHeight: .infinity)
                  .background(Color(nsColor: .windowBackgroundColor))
          } else {
              mainContent
          }
      }
      .frame(maxWidth: .infinity, maxHeight: .infinity)
      .overlay {
          if viewModel.showChatSettings, let chat = viewModel.currentChat {
              ChatSettingsView(
                  isPresented: $viewModel.showChatSettings,
                  chat: chat,
                  onSave: { title, instr, labels in
                      let updated = Chat(
                          id: chat.id, title: title, instructions: instr,
                          defaultModelId: chat.defaultModelId, labels: labels,
                          expandedTimestamps: Set(chat.expandedTimestamps.map { KotlinLong(longLong: $0) }),
                          history: chat.history.map { $0.toKotlinEntry() }
                      )
                      Task { try? await KotlinBridge.shared.chatsUseCase.update(chat: updated) }
                  }
              )
          }
      }
  }
  ```

  Extract the existing chat view into a `mainContent` computed property:

  ```swift
  private var mainContent: some View {
      VStack(spacing: 0) {
          VStack(spacing: 0) {
              HeaderView(
                  chatSearchQuery: $viewModel.chatSearchQuery,
                  chatDropdownExpanded: $chatDropdownExpanded,
                  chatName: $viewModel.chatName,
                  filteredChats: viewModel.filteredChats,
                  allChatsCount: viewModel.allChats.count,
                  onCreateNew: viewModel.createNewChat,
                  onDelete: viewModel.deleteCurrentChat,
                  onChatSettings: { viewModel.showChatSettings = true },
                  onSettings: { viewModel.showSettings = true },
                  onSelectChat: viewModel.selectChat
              )
              .padding(.horizontal, DesignTokens.sectionPadding)
              .padding(.top, DesignTokens.sectionPadding)
              .zIndex(1)

              Divider()
                  .padding(.vertical, DesignTokens.sectionPadding)

              PromptAreaView(
                  prompt: $viewModel.prompt,
                  loading: $viewModel.loading,
                  selectedModelName: selectedModelLabel,
                  selectedModelId: viewModel.selectedModel,
                  models: viewModel.models,
                  modelsEmpty: viewModel.models.isEmpty,
                  error: viewModel.error,
                  onSend: viewModel.send,
                  onCancel: viewModel.cancelSend,
                  onSelectModel: viewModel.selectModel,
                  onOpenSettings: { viewModel.showSettings = true }
              )
              .padding(.horizontal, DesignTokens.sectionPadding)
              .padding(.bottom, DesignTokens.sectionPadding)
          }
          .background(RoundedRectangle(cornerRadius: DesignTokens.topAreaCornerRadius).fill(DesignTokens.topAreaBackground))
          .frame(maxHeight: totalHeight * CGFloat(viewModel.splitFraction))
          .zIndex(2)
          .padding(.horizontal, 12)
          .padding(.top, 8)
          .padding(.bottom, 4)

          DraggableSplitter(
              fraction: $viewModel.splitFraction,
              totalHeight: totalHeight,
              onFractionChanged: viewModel.saveSplitFraction
          )

          let history = viewModel.currentChat?.history.reversed() ?? []
          ScrollView {
              LazyVStack(spacing: 0) {
                  ForEach(Array(history.enumerated()), id: \.element.id) { i, entry in
                      let isFirst = i == 0
                      let wordCount = entry.response.split { $0.isWhitespace }.count
                      let respExpanded = (viewModel.currentChat?.expandedTimestamps.contains(entry.timestamp) ?? false)
                          || isFirst
                          || wordCount < 50
                      let entryModel = viewModel.models.first { $0.id == entry.modelId }
                      let entryProviderName = viewModel.providers.first { $0.id == entryModel?.providerId }?.name
                      let entryModelLabel = entryProviderName != nil ? "\(entryProviderName!): \(entry.modelName)" : entry.modelName

                      ResponseCardView(
                          entry: entry,
                          isFirst: isFirst,
                          currentPrompt: viewModel.prompt,
                          currentModelId: viewModel.selectedModel,
                          isExpanded: respExpanded,
                          chatId: viewModel.currentChatId ?? "",
                          onToggleExpand: { viewModel.toggleEntryExpanded(timestamp: entry.timestamp, chatId: viewModel.currentChatId ?? "") },
                          onToggleMarkdown: { viewModel.toggleEntryMarkdown(timestamp: entry.timestamp, chatId: viewModel.currentChatId ?? "") },
                          onUseAsPrompt: viewModel.useAsPrompt,
                          onAppendToPrompt: viewModel.appendToPrompt,
                          onCopy: { NSPasteboard.general.clearContents(); NSPasteboard.general.setString($0, forType: .string) },
                          onRemove: { viewModel.removeEntry(at: (viewModel.currentChat?.history.count ?? 0) - 1 - i, chatId: viewModel.currentChatId ?? "") },
                          modelName: entryModelLabel
                      )
                      .padding(.horizontal, 12)
                      .padding(.top, i == 0 ? 0 : DesignTokens.cardVerticalPadding)
                      .padding(.bottom, i == history.count - 1 ? 0 : DesignTokens.cardVerticalPadding)
                  }
              }
          }
          .padding(.bottom, 8)
          .dotGridBackground()
      }
      .background(Color(nsColor: .windowBackgroundColor))
      .overlay(
          GeometryReader { geo in
              Color.clear
                  .onAppear { totalHeight = geo.size.height }
                  .onChange(of: geo.size.height) { totalHeight = $0 }
          }
      )
  }
  ```

- [ ] **Step 2: Remove `frame(minWidth:minHeight:)` from SettingsView**

  In `xcodeApp/Shared/Components/SettingsView.swift`, remove line 49:
  ```swift
  // Remove this line:
  .frame(minWidth: 500, minHeight: 400)
  ```

  SettingsView will now fill whatever space `MacMainChatView` gives it.

- [ ] **Step 3: Build and verify**

  Build macOS target. Open settings — it should fill the full window like Compose. Back button should return to chat.

- [ ] **Step 4: Commit**

  ```bash
  git add xcodeApp/macApp/Views/MacMainChatView.swift xcodeApp/Shared/Components/SettingsView.swift
  git commit -m "fix(ios): settings fills full window like Compose, replaces sheet modal"
  ```

---

## Task 2: Fix ChatsTabView — add ScrollView

**Files:**
- Modify: `xcodeApp/Shared/Components/ChatsTabView.swift:22-33`

`LazyVStack` renders outside a `ScrollView`, so with many chats the list overflows the settings panel and is not scrollable. Compose wraps this in `LazyColumn(Modifier.weight(1f))`.

- [ ] **Step 1: Wrap LazyVStack in ScrollView**

  In `xcodeApp/Shared/Components/ChatsTabView.swift`, replace `body`:

  ```swift
  var body: some View {
      VStack(spacing: 8) {
          if !allLabels.isEmpty {
              labelFilterView
          }
          ScrollView {
              LazyVStack(spacing: 0) {
                  ForEach(filteredChats) { chat in
                      ChatEditRowView(chat: chat, onUpdate: onUpdate, onDelete: onDelete)
                  }
              }
          }
          .frame(maxHeight: .infinity)
      }
      .frame(maxHeight: .infinity)
  }
  ```

- [ ] **Step 2: Build and verify**

  Build macOS target. Open Settings → Chats tab with multiple chats. List should scroll.

- [ ] **Step 3: Commit**

  ```bash
  git add xcodeApp/Shared/Components/ChatsTabView.swift
  git commit -m "fix(ios): ChatsTabView — wrap LazyVStack in ScrollView so list is scrollable"
  ```

---

## Task 3: Fix GlobalTabView — TextEditor fills available space

**Files:**
- Modify: `xcodeApp/Shared/Components/GlobalTabView.swift:7-27`

`TextEditor` with no frame constraint collapses to its intrinsic minimum height inside the VStack. Compose uses `Modifier.weight(1f)` to fill remaining space.

- [ ] **Step 1: Add frame(maxHeight:.infinity) to TextEditor and VStack**

  Replace the entire `body` in `xcodeApp/Shared/Components/GlobalTabView.swift`:

  ```swift
  var body: some View {
      VStack {
          TextEditor(text: Binding(
              get: { settingsVM.globalInstructions },
              set: { settingsVM.globalInstructions = $0; settingsVM.setGlobalInstructions($0) }
          ))
          .font(.body)
          .frame(maxHeight: .infinity)
          .overlay(
              Group {
                  if settingsVM.globalInstructions.isEmpty {
                      Text("Enter global instructions for all chats...")
                          .foregroundColor(.secondary)
                          .padding(.leading, 4)
                          .padding(.top, 8)
                          .allowsHitTesting(false)
                  }
              },
              alignment: .topLeading
          )
          .overlay(
              RoundedRectangle(cornerRadius: 4)
                  .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
          )
      }
      .frame(maxHeight: .infinity)
  }
  ```

  Also adds a border (matching how every other TextEditor in this codebase looks).

- [ ] **Step 2: Build and verify**

  Build macOS target. Open Settings → Global tab. TextEditor should fill the panel height.

- [ ] **Step 3: Commit**

  ```bash
  git add xcodeApp/Shared/Components/GlobalTabView.swift
  git commit -m "fix(ios): GlobalTabView — TextEditor fills available height, add border"
  ```

---

## Task 4: Minor visual fixes (DesignTokens + colors + bottom bar)

**Files:**
- Modify: `xcodeApp/Shared/DesignTokens.swift:4,6`
- Modify: `xcodeApp/Shared/Components/ProvidersTabView.swift:63`
- Modify: `xcodeApp/Shared/Components/ResponseCardView.swift:93-119`

- [ ] **Step 1: DesignTokens — raise topAreaBackground opacity and cardCornerRadius**

  ```swift
  // xcodeApp/Shared/DesignTokens.swift line 4
  // Before:
  static let topAreaBackground = Color.accentColor.opacity(0.08)
  // After:
  static let topAreaBackground = Color.accentColor.opacity(0.4)

  // line 6
  // Before:
  static let cardCornerRadius: CGFloat = 8
  // After:
  static let cardCornerRadius: CGFloat = 12
  ```

- [ ] **Step 2: ProviderCardView — use accentColor for key-set status**

  In `xcodeApp/Shared/Components/ProvidersTabView.swift`:
  ```swift
  // Before:
  .foregroundColor(hasKey ? .green : .red)
  // After:
  .foregroundColor(hasKey ? Color.accentColor : Color.red)
  ```

- [ ] **Step 3: ResponseCardView — fix bottom bar model label layout**

  Replace the `bottomBar` computed property in `xcodeApp/Shared/Components/ResponseCardView.swift`:

  ```swift
  private var bottomBar: some View {
      HStack(spacing: 0) {
          Button(action: onRemove) {
              Image("delete_forever")
                  .actionIcon()
          }
          .buttonStyle(.plain)
          .help("Remove from History")

          Text(formatTimestamp(entry.timestamp))
              .font(.caption2)
              .foregroundColor(.secondary)

          if let name = modelName {
              Text(" \u{00B7} \(name)")
                  .font(.caption2)
                  .foregroundColor(.accentColor)
                  .lineLimit(1)
                  .truncationMode(.tail)
                  .frame(maxWidth: .infinity, alignment: .leading)
          } else {
              Spacer()
          }

          Text(" \u{00B7} \(formatDuration(entry.durationMs))")
              .font(.caption2)
              .foregroundColor(.secondary)
      }
  }
  ```

- [ ] **Step 4: Build and verify all three**

  Build macOS target. Confirm: header area has visible blue tint, cards have larger corner radius, provider key-set shows blue not green, long model names truncate in bottom bar.

- [ ] **Step 5: Commit**

  ```bash
  git add xcodeApp/Shared/DesignTokens.swift \
          xcodeApp/Shared/Components/ProvidersTabView.swift \
          xcodeApp/Shared/Components/ResponseCardView.swift
  git commit -m "fix(ios): tint opacity, card radius, provider color, bottom bar truncation"
  ```

---

## Self-Review

**Spec coverage:**
- ✅ Settings full-screen (biggest gap) → Task 1
- ✅ ChatsTabView broken scroll → Task 2
- ✅ GlobalTabView collapsed height → Task 3
- ✅ Top area tint → Task 4
- ✅ Card corner radius → Task 4
- ✅ Provider status color → Task 4
- ✅ Bottom bar model truncation → Task 4

**Placeholder scan:** No TBDs. All code shown in full.

**Type consistency:** `DesignTokens.cardCornerRadius` change in Task 4 auto-propagates to `ResponseCardView` (already references `DesignTokens.cardCornerRadius`). No additional edits needed.
