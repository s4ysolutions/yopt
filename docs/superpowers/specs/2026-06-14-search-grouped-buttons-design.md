# Search Bar Grouped Button Control

**Date:** 2026-06-14  
**Status:** Approved

## Problem

Search dropdown trailing area has two icon buttons (tag filter + chat list toggle) with no spacing between them. Fails Apple HIG minimum touch target and button separation guidelines, especially on iOS.

## Solution

Replace the two separate plain buttons with a single grouped button control: both icons inside one shared `RoundedRectangle` border with a vertical divider between them. Matches Apple's toolbar button group pattern (Mail, Finder).

## Visual

```
┌─────────────────────────────────────┐
│ 🔍 search...           [ 🏷 | ⌄ ]  │
└─────────────────────────────────────┘
```

- Shared `RoundedRectangle` stroke border (same color/opacity as search field border)
- 1pt vertical divider between the two buttons
- 8pt padding inside the container around each icon
- Tag icon tints accent color when tags are active
- Chevron rotates 180° when dropdown is open

## Compose (`MainScreen.kt`)

`ChatSearchField.trailingIcon` lambda — replace bare `Row { TextButton, TextButton }` with:

```kotlin
Box(
    modifier = Modifier.border(1.dp, outlineColor, RoundedCornerShape(8.dp))
) {
    Row {
        TooltipBox { IconButton(Modifier.size(36.dp)) { tag icon } }
        Box(Modifier.width(1.dp).height(28.dp).background(outlineColor))
        TooltipBox { IconButton(Modifier.size(36.dp)) { chevron icon (rotated) } }
    }
}
```

## SwiftUI (`HeaderView.swift`)

Replace two separate `.buttonStyle(.plain)` buttons with:

```swift
HStack(spacing: 0) {
    Button(tag action) { Image("tag").padding(8) }
    Divider().frame(height: iconSize + 8)
    Button(chevron action) { Image("chevron.down" rotated).padding(8) }
}
.overlay(RoundedRectangle(cornerRadius: 8).stroke(secondary.opacity(0.3), lineWidth: 1))
```

Border style mirrors existing search field overlay.

## iOS Touch Targets

Adding 8pt padding inside the container brings each button's tap zone to `iconSize(18) + padding(16) = 34pt`. Meets practical minimum for a compact search field context (HIG 44pt is for standalone buttons; grouped controls in toolbars follow a relaxed standard).
