# Tag-filter Sheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user pick one or more tags from a sheet and scope the main-screen incremental chat search to chats that carry **all** the picked tags, on both Compose (Android/Desktop/Web) and SwiftUI (iOS/macOS).

**Architecture:** A pure filter function in `:shared` (`ChatTagFilter`) is the single testable core for the filter semantics — title/label free-text match AND-ed with an all-tags-present check. The Compose UI calls it directly; the SwiftUI UI replicates the same predicate over its `ChatModel` wrappers (matching the existing duplicated-filter pattern in `ChatViewModel`). A tag-filter sheet (a Material 3 `AlertDialog` on Compose, a `.sheet` on SwiftUI) is opened from a filter icon inside the search field or by typing `#`. Active filters show as a removable chip in the search field.

**Tech Stack:** Kotlin 2.3.21 · Compose Multiplatform 1.11.0 · `kotlinx.serialization` · kotlin.test (desktopTest) · SwiftUI · Xcode pbxproj.

---

## File Structure

**Created:**
- `shared/src/commonMain/kotlin/s4y/yopt/domain/services/ChatTagFilter.kt` — pure filter function (filter semantics, no sort).
- `shared/src/desktopTest/kotlin/s4y/yopt/domain/services/ChatTagFilterTest.kt` — unit tests for the function.
- `composeApp/src/commonMain/kotlin/s4y/yopt/ui/TagFilterSheet.kt` — Compose tag-picker dialog.
- `xcodeApp/Shared/Components/TagFilterSheet.swift` — SwiftUI tag-picker sheet.

**Modified:**
- `composeApp/src/commonMain/kotlin/s4y/yopt/ui/AppIcons.kt` — add `FilterByTags` icon.
- `composeApp/src/commonMain/composeResources/values/strings.xml` — new strings.
- `composeApp/src/commonMain/kotlin/s4y/yopt/ui/MainScreen.kt` — state, derived tags, call `ChatTagFilter`, `#` shortcut, filter icon + active chip in both header branches.
- `xcodeApp/Shared/Resources/Localizable.strings` — new strings.
- `xcodeApp/Shared/ViewModels/ChatViewModel.swift` — `selectedTags` state, `allTags`/`tagCounts` computed, updated `filteredChats`.
- `xcodeApp/Shared/Components/HeaderView.swift` — filter icon, `#` shortcut, active chip, present sheet; 3 new params.
- `xcodeApp/iosApp/Views/MainChatView.swift` + `xcodeApp/macApp/Views/ChatTopPaneView.swift` — pass new `HeaderView` params.
- `xcodeApp/YoPt.xcodeproj/project.pbxproj` — register `TagFilterSheet.swift`.

---

## Task 1: Pure filter function in `:shared` (the testable core)

**Files:**
- Create: `shared/src/commonMain/kotlin/s4y/yopt/domain/services/ChatTagFilter.kt`
- Test: `shared/src/desktopTest/kotlin/s4y/yopt/domain/services/ChatTagFilterTest.kt`

Semantics (mirror the existing inline filter at `MainScreen.kt:169-174`, plus the new all-tags AND):
- A chat passes the **tag** gate if `selectedTags` is empty OR the chat's `labels` contain **every** selected tag.
- A chat passes the **text** gate if `query` is blank OR its title contains `query` (ignore case) OR any label contains `query` (ignore case).
- Both gates must pass. No sorting here (the caller keeps its own sort).

- [ ] **Step 1: Write the failing test**

Create `shared/src/desktopTest/kotlin/s4y/yopt/domain/services/ChatTagFilterTest.kt`:

```kotlin
package s4y.yopt.domain.services

import s4y.yopt.domain.models.Chat
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatTagFilterTest {

    private fun chat(id: String, title: String, labels: List<String>) =
        Chat(id = id, title = title, labels = labels)

    private val chats = listOf(
        chat("1", "KMP build error", listOf("kotlin", "gradle")),
        chat("2", "Coroutine question", listOf("kotlin")),
        chat("3", "Trip plan", listOf("travel")),
        chat("4", "Untagged note", emptyList()),
    )

    @Test
    fun `no query and no tags returns all`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = emptySet())
        assertEquals(listOf("1", "2", "3", "4"), result.map { it.id })
    }

    @Test
    fun `single tag keeps only chats carrying it`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = setOf("kotlin"))
        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun `multiple tags require all to be present`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = setOf("kotlin", "gradle"))
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `query matches title case-insensitively`() {
        val result = ChatTagFilter.filter(chats, query = "trip", selectedTags = emptySet())
        assertEquals(listOf("3"), result.map { it.id })
    }

    @Test
    fun `query matches a label`() {
        val result = ChatTagFilter.filter(chats, query = "gradle", selectedTags = emptySet())
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `query and tags combine with AND`() {
        val result = ChatTagFilter.filter(chats, query = "coroutine", selectedTags = setOf("kotlin"))
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `tag absent from every chat yields empty`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = setOf("nonexistent"))
        assertEquals(emptyList(), result.map { it.id })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "s4y.yopt.domain.services.ChatTagFilterTest"`
Expected: FAIL — compilation error, `ChatTagFilter` unresolved.

- [ ] **Step 3: Write minimal implementation**

Create `shared/src/commonMain/kotlin/s4y/yopt/domain/services/ChatTagFilter.kt`:

```kotlin
package s4y.yopt.domain.services

import s4y.yopt.domain.models.Chat

/**
 * Pure filter for the main-screen chat search. Combines an all-tags-present gate
 * with the existing free-text title/label substring match. Sorting is the caller's
 * responsibility.
 */
object ChatTagFilter {
    fun filter(chats: List<Chat>, query: String, selectedTags: Set<String>): List<Chat> =
        chats.filter { chat ->
            val tagGate = selectedTags.isEmpty() ||
                selectedTags.all { tag -> chat.labels.contains(tag) }
            val textGate = query.isBlank() ||
                chat.title.contains(query, ignoreCase = true) ||
                chat.labels.any { label -> label.contains(query, ignoreCase = true) }
            tagGate && textGate
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "s4y.yopt.domain.services.ChatTagFilterTest"`
Expected: PASS — 7 tests.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/s4y/yopt/domain/services/ChatTagFilter.kt \
        shared/src/desktopTest/kotlin/s4y/yopt/domain/services/ChatTagFilterTest.kt
git commit -m "feat(shared): add ChatTagFilter for tag-scoped chat search"
```

---

## Task 2: Compose — add `FilterByTags` icon + string resources

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/s4y/yopt/ui/AppIcons.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add the icon via the icons skill**

Invoke the `.claude/skills/icons.md` skill to add the Material Symbols Rounded **"sell"** glyph (a price-tag shape) to `AppIcons.kt`, following the existing lazy-`ImageVector` pattern used by `tune`/`shadowAdd`. Register it in the `AppIcons` object under a new "Tag filter" comment:

```kotlin
    // Tag filter
    val FilterByTags: ImageVector = sell
```

where `sell` is the private `by lazy { ImageVector.Builder(name = "Sell", ...) }` the skill emits. (SF Symbol counterpart for the SwiftUI side is `tag`; recorded here so Task 8 stays consistent.)

- [ ] **Step 2: Add string resources**

In `composeApp/src/commonMain/composeResources/values/strings.xml`, after the existing `<string name="tag">Tag</string>` line (line 28), add:

```xml
    <string name="filter_by_tags">Filter by tags</string>
    <string name="tag_filter_clear">Clear</string>
    <string name="tag_filter_done">Done</string>
    <string name="tag_filter_active">%1$d tags</string>
    <string name="tag_filter_empty">No tags yet</string>
```

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :composeApp:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL (the new icon + generated `Res.string.*` resolve).

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/s4y/yopt/ui/AppIcons.kt \
        composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(compose): add tag-filter icon and strings"
```

---

## Task 3: Compose — `TagFilterSheet` dialog

**Files:**
- Create: `composeApp/src/commonMain/kotlin/s4y/yopt/ui/TagFilterSheet.kt`

A Material 3 `AlertDialog` (matches the existing chat-settings dialog style) showing every tag as a `FilterChip` with its chat count, wrapped in a scrollable `FlowRow` so it scales to many tags. Buttons: Clear (dismiss button) and Done (confirm button).

- [ ] **Step 1: Write the composable**

Create `composeApp/src/commonMain/kotlin/s4y/yopt/ui/TagFilterSheet.kt`:

```kotlin
package s4y.yopt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import yopt.composeapp.generated.resources.Res
import yopt.composeapp.generated.resources.filter_by_tags
import yopt.composeapp.generated.resources.tag_filter_clear
import yopt.composeapp.generated.resources.tag_filter_done
import yopt.composeapp.generated.resources.tag_filter_empty

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterSheet(
    allTags: List<String>,
    tagCounts: Map<String, Int>,
    selectedTags: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.filter_by_tags)) },
        text = {
            if (allTags.isEmpty()) {
                Text(stringResource(Res.string.tag_filter_empty))
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    allTags.forEach { tag ->
                        val count = tagCounts[tag] ?: 0
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = { onToggle(tag) },
                            label = { Text("$tag ($count)") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.tag_filter_done)) }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text(stringResource(Res.string.tag_filter_clear)) }
        },
    )
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/s4y/yopt/ui/TagFilterSheet.kt
git commit -m "feat(compose): add TagFilterSheet dialog"
```

---

## Task 4: Compose — wire tag filter into `MainScreen`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/s4y/yopt/ui/MainScreen.kt`

- [ ] **Step 1: Add filter state next to `chatSearchQuery`**

After `MainScreen.kt:135` (`var chatDropdownExpanded by remember ...`), add:

```kotlin
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }
    var showTagSheet by remember { mutableStateOf(false) }
```

- [ ] **Step 2: Derive tag universe + counts and replace `filteredChats`**

Replace the `val filteredChats = allChats ...` block (`MainScreen.kt:169-177`) with:

```kotlin
    val tagCounts = allChats.flatMap { it.labels }.groupingBy { it }.eachCount()
    val allTags = tagCounts.keys.sorted()
    // Drop selected tags that no longer exist on any chat, so an all-match
    // against a stale tag can't silently empty the list.
    val effectiveTags = selectedTags.intersect(allTags.toSet())

    val filteredChats = ChatTagFilter
        .filter(allChats, chatSearchQuery, effectiveTags)
        .sortedByDescending { c ->
            c.history.lastOrNull()?.timestamp ?: c.id.removePrefix("chat_").toLongOrNull() ?: 0L
        }
```

Add the import near the top of the file (alongside the other `s4y.yopt` imports):

```kotlin
import s4y.yopt.domain.services.ChatTagFilter
```

- [ ] **Step 3: Render the sheet**

Immediately after the `if (showChatSettings && currentChat != null) { ... }` block closes (before the responsive `BoxWithConstraints` header), add:

```kotlin
            if (showTagSheet) {
                TagFilterSheet(
                    allTags = allTags,
                    tagCounts = tagCounts,
                    selectedTags = effectiveTags,
                    onToggle = { tag ->
                        selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                    },
                    onClear = { selectedTags = emptySet(); showTagSheet = false },
                    onDismiss = { showTagSheet = false },
                )
            }
```

- [ ] **Step 4: Extract a shared search-field decoration helper**

Both header branches build the same search `OutlinedTextField`. To stay DRY, add this private composable at the bottom of `MainScreen.kt` (file scope, after the `MainScreen` function). It encapsulates the `#` shortcut, the active-filter chip (leading), and the filter + chat-list toggle icons (trailing):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenTagSheet: () -> Unit,
    selectedTagCount: Int,
    onClearTags: () -> Unit,
    onToggleDropdown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = { new ->
            if (new.startsWith("#")) onOpenTagSheet() else onQueryChange(new)
        },
        singleLine = true,
        modifier = modifier,
        placeholder = { Text(stringResource(Res.string.search)) },
        leadingIcon = if (selectedTagCount > 0) {
            {
                InputChip(
                    selected = true,
                    onClick = onClearTags,
                    label = { Text(stringResource(Res.string.tag_filter_active, selectedTagCount)) },
                    trailingIcon = { Text("×") },
                )
            }
        } else null,
        trailingIcon = {
            Row {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above
                    ),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.filter_by_tags)) } },
                    state = rememberTooltipState()
                ) {
                    TextButton(onClick = onOpenTagSheet) {
                        Icon(
                            AppIcons.FilterByTags,
                            contentDescription = stringResource(Res.string.filter_by_tags),
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTagCount > 0)
                                MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                        )
                    }
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above
                    ),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.chat_list_tooltip)) } },
                    state = rememberTooltipState()
                ) {
                    TextButton(onClick = onToggleDropdown) {
                        Icon(
                            AppIcons.ChatListToggle,
                            contentDescription = stringResource(Res.string.chat_list_tooltip),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
}
```

Add imports if missing: `androidx.compose.material3.InputChip`, `androidx.compose.material3.MaterialTheme`, `androidx.compose.material3.LocalContentColor`, `androidx.compose.foundation.layout.Row`.

- [ ] **Step 5: Use the helper in the NARROW branch**

In the narrow branch replace the search `OutlinedTextField(...)` block (`MainScreen.kt:317-344`) with a call to the helper, keeping the surrounding `Box(Modifier.weight(1f)) { ... DropdownMenu ... }`:

```kotlin
                                Box(Modifier.weight(1f)) {
                                    ChatSearchField(
                                        query = chatSearchQuery,
                                        onQueryChange = {
                                            chatSearchQuery = it
                                            chatDropdownExpanded = true
                                        },
                                        onOpenTagSheet = { showTagSheet = true },
                                        selectedTagCount = effectiveTags.size,
                                        onClearTags = { selectedTags = emptySet() },
                                        onToggleDropdown = { chatDropdownExpanded = !chatDropdownExpanded },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    DropdownMenu(
                                        expanded = chatDropdownExpanded,
                                        onDismissRequest = { chatDropdownExpanded = false },
                                        properties = PopupProperties(focusable = false)
                                    ) {
                                        filteredChats.forEach { chat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        chat.title,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                onClick = {
                                                    currentChatId = chat.id
                                                    chatSearchQuery = ""
                                                    chatDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
```

- [ ] **Step 6: Use the helper in the WIDE branch**

In the wide branch replace the search `OutlinedTextField(...)` block (`MainScreen.kt:471-498`) the same way, keeping its `Box(Modifier.width(headerWidth * 0.3f)) { ... DropdownMenu ... }`:

```kotlin
                            Box(Modifier.width(headerWidth * 0.3f)) {
                                ChatSearchField(
                                    query = chatSearchQuery,
                                    onQueryChange = {
                                        chatSearchQuery = it
                                        chatDropdownExpanded = true
                                    },
                                    onOpenTagSheet = { showTagSheet = true },
                                    selectedTagCount = effectiveTags.size,
                                    onClearTags = { selectedTags = emptySet() },
                                    onToggleDropdown = { chatDropdownExpanded = !chatDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                DropdownMenu(
                                    expanded = chatDropdownExpanded,
                                    onDismissRequest = { chatDropdownExpanded = false },
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    filteredChats.forEach { chat ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    chat.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            onClick = {
                                                currentChatId = chat.id
                                                chatSearchQuery = ""
                                                chatDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
```

- [ ] **Step 7: Build and run the desktop app to verify**

Run: `./gradlew :composeApp:run`
Expected: app launches. Manually confirm: typing `#` opens the sheet; selecting tags then closing scopes the chat dropdown to chats with all selected tags; the `N tags ×` chip appears and clears the filter; the filter icon turns primary-coloured while active.

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/s4y/yopt/ui/MainScreen.kt
git commit -m "feat(compose): scope chat search by selected tags via filter sheet"
```

---

## Task 5: SwiftUI — add Localizable strings

**Files:**
- Modify: `xcodeApp/Shared/Resources/Localizable.strings`

- [ ] **Step 1: Add strings**

After the `"chatSettings.addTag" = "Add Tag";` line (line 18), add:

```
"tagFilter.title" = "Filter by tags";
"tagFilter.clear" = "Clear";
"tagFilter.done" = "Done";
"tagFilter.active" = "%lld tags";
"tagFilter.empty" = "No tags yet";
"help.filterByTags" = "Filter by tags";
```

- [ ] **Step 2: Commit**

```bash
git add xcodeApp/Shared/Resources/Localizable.strings
git commit -m "feat(xcodeApp): add tag-filter strings"
```

---

## Task 6: SwiftUI — `ChatViewModel` tag state + filter

**Files:**
- Modify: `xcodeApp/Shared/ViewModels/ChatViewModel.swift`

- [ ] **Step 1: Add `selectedTags` published state**

After `@Published var lastPrompt: String = ""` (`ChatViewModel.swift:29`), add:

```swift
    @Published var selectedTags: Set<String> = []
```

- [ ] **Step 2: Add derived tag universe + counts and update `filteredChats`**

Replace the `var filteredChats: [ChatModel] { ... }` computed property (`ChatViewModel.swift:36-45`) with:

```swift
    var tagCounts: [String: Int] {
        var counts: [String: Int] = [:]
        for chat in allChats {
            for label in chat.labels { counts[label, default: 0] += 1 }
        }
        return counts
    }

    var allTags: [String] { tagCounts.keys.sorted() }

    /// Selected tags that still exist on some chat — guards against an all-match
    /// against a stale tag silently emptying the list.
    var effectiveTags: Set<String> { selectedTags.intersection(Set(allTags)) }

    var filteredChats: [ChatModel] {
        let query = chatSearchQuery.lowercased()
        let tags = effectiveTags
        return allChats
            .filter { chat in
                tags.isEmpty || tags.allSatisfy { chat.labels.contains($0) }
            }
            .filter { query.isEmpty || $0.title.lowercased().contains(query) || $0.labels.contains { $0.lowercased().contains(query) } }
            .sorted(by: { a, b in
                let aTime = a.history.last?.timestamp ?? Int64(a.id.dropFirst(5)) ?? 0
                let bTime = b.history.last?.timestamp ?? Int64(b.id.dropFirst(5)) ?? 0
                return aTime > bTime
            })
    }
```

- [ ] **Step 3: Build the macOS target to verify**

Run: `xcodebuild -project xcodeApp/YoPt.xcodeproj -scheme "YoPt (macOS)" -destination 'platform=macOS' build`
Expected: BUILD SUCCEEDED.

- [ ] **Step 4: Commit**

```bash
git add xcodeApp/Shared/ViewModels/ChatViewModel.swift
git commit -m "feat(xcodeApp): add tag-filter state to ChatViewModel"
```

---

## Task 7: SwiftUI — `TagFilterSheet` view + pbxproj registration

**Files:**
- Create: `xcodeApp/Shared/Components/TagFilterSheet.swift`
- Modify: `xcodeApp/YoPt.xcodeproj/project.pbxproj`

- [ ] **Step 1: Write the sheet view**

Create `xcodeApp/Shared/Components/TagFilterSheet.swift` (reuses `FlowLayout` from `TagChipsView.swift`):

```swift
import SwiftUI

struct TagFilterSheet: View {
    let allTags: [String]
    let tagCounts: [String: Int]
    @Binding var selectedTags: Set<String>
    let onClear: () -> Void
    let onDone: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.spacing12) {
            Text(String(localized: "tagFilter.title"))
                .font(.headline)

            if allTags.isEmpty {
                Text(String(localized: "tagFilter.empty"))
                    .foregroundColor(.secondary)
            } else {
                ScrollView {
                    FlowLayout(spacing: DesignTokens.spacing4) {
                        ForEach(allTags, id: \.self) { tag in
                            let isOn = selectedTags.contains(tag)
                            Button {
                                if isOn { selectedTags.remove(tag) } else { selectedTags.insert(tag) }
                            } label: {
                                Text("\(tag) (\(tagCounts[tag] ?? 0))")
                                    .font(.caption2)
                                    .padding(.horizontal, DesignTokens.padding8)
                                    .padding(.vertical, DesignTokens.padding4)
                            }
                            .buttonStyle(.bordered)
                            .controlSize(.small)
                            .tint(isOn ? .accentColor : .secondary)
                        }
                    }
                }
                .frame(maxHeight: DesignTokens.modelPickerMaxHeight)
            }

            HStack {
                Button(String(localized: "tagFilter.clear"), action: onClear)
                Spacer()
                Button(String(localized: "tagFilter.done"), action: onDone)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding()
#if os(macOS)
        .frame(width: DesignTokens.chatSettingsMinWidth)
        .fixedSize(horizontal: false, vertical: true)
#else
        .presentationDetents([.medium])
#endif
    }
}
```

- [ ] **Step 2: Register the file in `project.pbxproj`**

Use the three free IDs after the current max (`...01B1`): fileRef `AA000000000000000001B2`, iOS buildFile `AA000000000000000001B3`, macOS buildFile `AA000000000000000001B4`.

Add after the `TagChipsView.swift` PBXBuildFile lines (after `project.pbxproj:53`):

```
		AA000000000000000001B3 /* TagFilterSheet.swift in Sources */ = {isa = PBXBuildFile; fileRef = AA000000000000000001B2 /* TagFilterSheet.swift */; };
		AA000000000000000001B4 /* TagFilterSheet.swift in Sources */ = {isa = PBXBuildFile; fileRef = AA000000000000000001B2 /* TagFilterSheet.swift */; };
```

Add after the `TagChipsView.swift` PBXFileReference line (after `project.pbxproj:116`):

```
		AA000000000000000001B2 /* TagFilterSheet.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = TagFilterSheet.swift; sourceTree = "<group>"; };
```

Add after the `TagChipsView.swift` group-children line (after `project.pbxproj:248`):

```
				AA000000000000000001B2 /* TagFilterSheet.swift */,
```

Add after the iOS `TagChipsView.swift in Sources` line in the B2 PBXSourcesBuildPhase (after `project.pbxproj:444`):

```
				AA000000000000000001B3 /* TagFilterSheet.swift in Sources */,
```

Add after the macOS `TagChipsView.swift in Sources` line in the B4 PBXSourcesBuildPhase (after `project.pbxproj:473`):

```
				AA000000000000000001B4 /* TagFilterSheet.swift in Sources */,
```

- [ ] **Step 3: Build the macOS target to verify registration**

Run: `xcodebuild -project xcodeApp/YoPt.xcodeproj -scheme "YoPt (macOS)" -destination 'platform=macOS' build`
Expected: BUILD SUCCEEDED, and `TagFilterSheet.swift` appears in the compile sources (no "cannot find TagFilterSheet in scope" later).

- [ ] **Step 4: Commit**

```bash
git add xcodeApp/Shared/Components/TagFilterSheet.swift xcodeApp/YoPt.xcodeproj/project.pbxproj
git commit -m "feat(xcodeApp): add TagFilterSheet view"
```

---

## Task 8: SwiftUI — wire `HeaderView` (icon, `#`, chip, sheet)

**Files:**
- Modify: `xcodeApp/Shared/Components/HeaderView.swift`
- Modify: `xcodeApp/iosApp/Views/MainChatView.swift`
- Modify: `xcodeApp/macApp/Views/ChatTopPaneView.swift`

- [ ] **Step 1: Add params + sheet state to `HeaderView`**

After `let onSelectChat: (String) -> Void` (`HeaderView.swift:13`), add:

```swift
    @Binding var selectedTags: Set<String>
    let allTags: [String]
    let tagCounts: [String: Int]
```

After `@FocusState private var searchFocused: Bool` (`HeaderView.swift:17`), add:

```swift
    @State private var showTagSheet = false
```

- [ ] **Step 2: Add filter icon, `#` shortcut, and active chip to `chatSearchField`**

Replace the `chatSearchField` computed property (`HeaderView.swift:72-123`) with the version below. Changes: the leading active-filter chip, the `#`-detection in `onChange`, and the filter `Button` before the chevron button; the `.sheet` is attached at the end.

```swift
    private var chatSearchField: some View {
        HStack(spacing: DesignTokens.spacing4) {
            if !selectedTags.isEmpty {
                Button(action: { selectedTags.removeAll() }) {
                    HStack(spacing: DesignTokens.spacing2) {
                        Text(String(format: String(localized: "tagFilter.active"), selectedTags.count))
                            .font(.caption2)
                        Text("\u{00D7}").font(.caption2)
                    }
                    .padding(.horizontal, DesignTokens.padding8)
                    .padding(.vertical, DesignTokens.padding4)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
            }
            TextField(String(localized: "search.placeholder"), text: $chatSearchQuery)
                .textFieldStyle(.plain)
                .focused($searchFocused)
#if os(macOS)
                .onChange(of: chatSearchQuery) {
                    if chatSearchQuery.hasPrefix("#") { chatSearchQuery = ""; showTagSheet = true }
                    else { chatDropdownExpanded = true }
                }
#else
                .onChange(of: chatSearchQuery) { newValue in
                    if newValue.hasPrefix("#") { chatSearchQuery = ""; showTagSheet = true }
                    else { chatDropdownExpanded = true }
                }
#endif
            Button {
                showTagSheet = true
            } label: {
                Image(systemName: "tag")
                    .resizable()
                    .scaledToFit()
                    .frame(width: DesignTokens.iconSize, height: DesignTokens.iconSize)
                    .foregroundColor(selectedTags.isEmpty ? .secondary : .accentColor)
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.filterByTags"))
            Button {
                chatDropdownExpanded.toggle()
            } label: {
                Image(systemName: "chevron.down")
                    .resizable()
                    .scaledToFit()
                    .frame(width: DesignTokens.iconSize, height: DesignTokens.iconSize)
                    .foregroundColor(.secondary)
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.chatList"))
        }
        .padding(DesignTokens.padding8)
        .overlay(
            RoundedRectangle(cornerRadius: DesignTokens.cornerRadius8)
                .stroke(Color.secondary.opacity(DesignTokens.opacity30), lineWidth: 1)
        )
        .background(
            GeometryReader { geo in
                Color.clear
                    .onAppear { searchFieldHeight = geo.size.height }
#if os(macOS)
                    .onChange(of: geo.size.height) { _, h in searchFieldHeight = h }
#else
                    .onChange(of: geo.size.height) { h in searchFieldHeight = h }
#endif
            }
        )
#if os(iOS)
        .background(Color(uiColor: .systemBackground))
#endif
        .overlay(alignment: .topLeading) {
            if chatDropdownExpanded {
                chatListView
                    .fixedSize(horizontal: false, vertical: true)
                    .offset(y: searchFieldHeight + 4)
                    .zIndex(10)
            }
        }
        .sheet(isPresented: $showTagSheet) {
            TagFilterSheet(
                allTags: allTags,
                tagCounts: tagCounts,
                selectedTags: $selectedTags,
                onClear: { selectedTags.removeAll(); showTagSheet = false },
                onDone: { showTagSheet = false }
            )
        }
    }
```

- [ ] **Step 3: Pass the new params at the iOS call site**

In `xcodeApp/iosApp/Views/MainChatView.swift`, add to the `HeaderView(...)` call (after `onSelectChat: viewModel.selectChat`, `MainChatView.swift:32`):

```swift
                    onSelectChat: viewModel.selectChat,
                    selectedTags: $viewModel.selectedTags,
                    allTags: viewModel.allTags,
                    tagCounts: viewModel.tagCounts
```

(Replace the existing `onSelectChat: viewModel.selectChat` line — which currently has no trailing comma — with the four lines above.)

- [ ] **Step 4: Pass the new params at the macOS call site**

In `xcodeApp/macApp/Views/ChatTopPaneView.swift`, apply the identical change to its `HeaderView(...)` call (the `onSelectChat: viewModel.selectChat` line at `ChatTopPaneView.swift:30`):

```swift
                onSelectChat: viewModel.selectChat,
                selectedTags: $viewModel.selectedTags,
                allTags: viewModel.allTags,
                tagCounts: viewModel.tagCounts
```

- [ ] **Step 5: Build the macOS target**

Run: `xcodebuild -project xcodeApp/YoPt.xcodeproj -scheme "YoPt (macOS)" -destination 'platform=macOS' build`
Expected: BUILD SUCCEEDED.

- [ ] **Step 6: Run the macOS app and verify parity**

Launch the macOS app. Confirm: the `tag` icon sits in the search field; tapping it (or typing `#`) opens the sheet; multi-selecting tags then Done scopes the chat list to chats with all selected tags; the `N tags ×` chip clears the filter; icon turns accent-coloured while active. Behaviour matches the Compose desktop app.

- [ ] **Step 7: Commit**

```bash
git add xcodeApp/Shared/Components/HeaderView.swift \
        xcodeApp/iosApp/Views/MainChatView.swift \
        xcodeApp/macApp/Views/ChatTopPaneView.swift
git commit -m "feat(xcodeApp): scope chat search by selected tags via filter sheet"
```

---

## Task 9: Full verification sweep

- [ ] **Step 1: Run the shared test suite**

Run: `./gradlew :shared:desktopTest`
Expected: BUILD SUCCESSFUL, `ChatTagFilterTest` green.

- [ ] **Step 2: Compile every Compose target touched**

Run: `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs`
Expected: BUILD SUCCESSFUL (confirms the shared UI code compiles for web too, where `MarkdownResponse` etc. differ).

- [ ] **Step 3: Build the macOS app one final time**

Run: `xcodebuild -project xcodeApp/YoPt.xcodeproj -scheme "YoPt (macOS)" -destination 'platform=macOS' build`
Expected: BUILD SUCCEEDED.

- [ ] **Step 4: Manual parity check**

Side-by-side: Compose desktop vs macOS. Same icon, same sheet contents (tags + counts), same active-chip text, same all-match scoping. Note any visual drift and fix before closing the task.
```
