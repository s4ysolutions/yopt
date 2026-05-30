---
name: icons
description: Add Material Symbols icons to AppIcons.kt from gstatic URLs.
---

# Add Icon Skill

Add a new Material Symbols Outlined icon to `AppIcons.kt` from a `fonts.gstatic.com` URL.

## Steps

### 1. Fetch and decompress

The gstatic URL returns a gzipped Kotlin source file. Fetch and decompress in one step:

```bash
curl -s '<URL>' | gunzip
```

The output is a Kotlin file containing an `ImageVector` definition with path data, plus a private backing field.

### 2. Add to AppIcons.kt

File: `composeApp/src/commonMain/kotlin/s4y/yopt/ui/AppIcons.kt`

**Object property** — add a semantic name to the `AppIcons` object in the appropriate section:

```kotlin
val SemanticName: ImageVector = iconVarName
```

The `iconVarName` is derived from the `name` parameter in the fetched `ImageVector.Builder` (e.g. `tune`, `content_copy`), converted to camelCase.

**Lazy val** — add the `ImageVector.Builder` block as a private lazy val below the object. Paste the entire builder block, adjusting:
- `name` in the Builder: use PascalCase (e.g. `name = "ContentCopy"`)
- The backing field pattern: replace the `get()` + private `var` pattern with `by lazy { ... }`
- Remove the package declaration and imports (already present in AppIcons.kt)

Pattern:

```kotlin
private val iconVarName: ImageVector by lazy {
    ImageVector.Builder(
        name = "PascalCase",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(androidx.compose.ui.graphics.Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            // path commands from fetched source
        }
    }.build()
}
```

### 3. Remove unused Material Icons imports

If the new icon replaces a `Icons.Rounded.Xxx` or `Icons.Filled.Xxx`, remove the now-unused import. Check if any other property still references that import before removing.

### 4. Verify build

```bash
./gradlew :composeApp:compileKotlinDesktop
```

## Naming conventions

- **Object property**: semantic name describing the action (`CopyToClipboard`), not the icon shape (`Copy`)
- **Lazy val**: camelCase of the Material Symbol name (`contentCopy`, `tune`, `rawOn`)
