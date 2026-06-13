# Tag-filter sheet for the main screen

**Date:** 2026-06-13
**Status:** Approved (design)

## Problem

Chats carry tags (`Chat.labels: List<String>`), edited in the chat-settings
dialog. On the main screen tags are nearly invisible: the only use is that the
incremental search field matches a tag as a substring. As the chat list grows,
there is no way to narrow it to a set of tags.

## Goal

Let the user pick one or more tags and scope the incremental chat search to
chats that carry **all** the picked tags. Constraints from the user:

- The main screen must not gain permanent clutter — no always-visible tag bar.
- Must work and look the same on Compose (Android/Desktop/Web) and SwiftUI
  (iOS/macOS, `xcodeApp`).
- Must scale to many tags.

## Design

### Interaction

1. A **filter icon** sits inside the existing search field (the trailing-icon
   slot already used by the chat-list toggle — a second small icon, no new row).
2. Tapping the icon — **or typing `#` as the first character** in the search
   field — opens a **tag-filter sheet** (modal). The `#` is not kept in the
   query; it is purely a shortcut to open the sheet.
3. The sheet shows every distinct tag as a multi-select chip with the count of
   chats carrying it. Tapping a chip toggles selection. Buttons: **Clear** and
   **Done**.
4. After **Done**, if any tags are selected:
   - The incremental search is scoped to chats carrying **all** selected tags
     (AND / "all-match"). Within that subset, typed text matches titles as
     today.
   - A removable **`N tags ✕`** chip appears at the start of the search field.
     Tapping `✕` clears the tag filter.
   - The filter icon shows a small dot/badge while a filter is active.
5. With no tags selected, search behaves exactly as today.

### Match semantics

```
filteredChats =
  allChats
    .filter { selectedTags.isEmpty() ||
              selectedTags.all { t -> it.labels.contains(t) } }   // ALL-match
    .filter { query.isBlank() ||
              it.title.contains(query, ignoreCase = true) ||
              it.labels.any { l -> l.contains(query, ignoreCase = true) } }
    .sortedByDescending { /* existing sort */ }
```

The existing free-text title/label substring match is kept unchanged; the tag
filter is simply AND-ed on top.

### State

- `selectedTags: Set<String>` — local UI state in `MainScreen`
  (`remember { mutableStateOf(emptySet()) }`). Transient: a filter, not
  persisted across restarts.
- `showTagSheet: Boolean` — local UI state controlling the sheet.
- The tag universe and counts are **derived** from the already-observed
  `allChats` in the composable:
  `allChats.flatMap { it.labels }.groupingBy { it }.eachCount()`.
  No new port, repository, or use case — this is read-only derivation over data
  the screen already has.

### Components

**Compose (`composeApp`):**
- New tag-sheet composable (Material 3 `ModalBottomSheet` on touch sizes, or an
  `AlertDialog` — match what the chat-settings dialog already uses for parity;
  reuse the existing tag-chip style: 28.dp `OutlinedButton`, selected state
  filled). Tags wrap in a scrollable area to scale to many tags.
- New filter icon constant in `AppIcons.kt` (Material Symbols Rounded), wrapped
  in `TooltipBox` per the icon-button rule.
- `#`-shortcut handled in the search field `onValueChange`: if the new value
  starts with `#`, open the sheet and drop the `#` from the query.
- The `N tags ✕` chip rendered as a `leadingIcon`/prefix in the search field
  (or a small chip in the `Row` before the field on narrow layout).
- Wire into both the narrow and wide header branches of the responsive layout.

**SwiftUI (`xcodeApp`) — visual parity required:**
- Mirror the sheet as a native `.sheet`, the filter icon, the active-filter
  chip, and the `#` shortcut.
- Use `DesignTokens.*` for colours/radii/padding/icon sizes — no hardcoding.
- Any new Swift file must be registered in `project.pbxproj` (PBXFileReference,
  group entry, two PBXBuildFile + PBXSourcesBuildPhase entries for targets B2
  iOS and B4 macOS) — see `.claude/skills/ui.md`.
- Map the new icon per the SF Symbol ↔ AppIcons table in `.claude/skills/ui.md`.

### i18n

New user-visible strings added to `values/strings.xml` and used via
`stringResource`:
- `filter_by_tags` — sheet title / icon tooltip
- `tag_filter_clear` — "Clear"
- `tag_filter_done` — "Done"
- `tag_filter_active` — "{count} tags" (active-filter chip; plural-aware)

### Error / edge cases

- No tags exist anywhere → sheet shows an empty-state line ("No tags yet"); the
  filter icon may still open it. Optionally hide the icon when no chat has tags.
- Selecting tags that together match zero chats → search list is empty; the
  active-filter chip still shows so the user can clear it.
- A selected tag is removed from all chats (via chat-settings) while the filter
  is active → it simply stops matching; stale selected tags that no longer exist
  are dropped from `selectedTags` on recomputation.

## Out of scope

- Persisting the tag filter across restarts.
- Any/All toggle (hardcoded ALL).
- Per-row tag chips in the chat dropdown (rejected: hard to mirror on
  iOS/macOS, breaks with many tags).
- Tag rename/colour/management UI (tags are still created in chat-settings).
```
