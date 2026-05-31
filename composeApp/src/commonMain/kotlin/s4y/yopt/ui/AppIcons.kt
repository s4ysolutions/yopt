package s4y.yopt.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Central icon theme — change a symbol here to update it across the entire app. */
object AppIcons {
    // Navigation
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val Settings: ImageVector = Icons.Rounded.Settings

    // Chat management
    val NewChat: ImageVector = Icons.Rounded.Add
    val DeleteChat: ImageVector = deleteForever
    val ChatInstructions: ImageVector = tune
    val ChatListToggle: ImageVector = Icons.Rounded.KeyboardArrowDown

    // Expand / collapse
    val Expand: ImageVector = expandContent
    val Collapse: ImageVector = collapseContent

    // Prompt & response actions
    val UseAsPrompt: ImageVector = rectangleAdd
    val AppendToPrompt: ImageVector = shadowAdd
    val CopyToClipboard: ImageVector = contentCopy
    val MarkdownView: ImageVector = markdown
    val RawView: ImageVector = rawOn
    val RemoveFromHistory: ImageVector = deleteForever

    // Provider actions
    val RefreshModels: ImageVector = Icons.Rounded.Refresh
}

private val shadowAdd: ImageVector by lazy {
    ImageVector.Builder(
        name = "ShadowAdd",
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
            moveTo(13f, 14f)
            verticalLineTo(11f)
            horizontalLineTo(10f)
            verticalLineTo(9f)
            horizontalLineToRelative(3f)
            verticalLineTo(6f)
            horizontalLineToRelative(2f)
            verticalLineTo(9f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(2f)
            horizontalLineTo(15f)
            verticalLineToRelative(3f)
            horizontalLineTo(13f)
            close()
            moveTo(4f, 22f)
            quadTo(3.18f, 22f, 2.59f, 21.41f)
            reflectiveQuadTo(2f, 20f)
            verticalLineTo(8f)
            quadTo(2f, 7.18f, 2.59f, 6.59f)
            reflectiveQuadTo(4f, 6f)
            horizontalLineTo(6f)
            verticalLineTo(4f)
            quadTo(6f, 3.17f, 6.59f, 2.59f)
            reflectiveQuadTo(8f, 2f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(22f, 4f)
            verticalLineTo(16f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 18f)
            horizontalLineTo(18f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(16f, 22f)
            horizontalLineTo(4f)
            close()
            moveTo(8f, 16f)
            horizontalLineTo(20f)
            verticalLineTo(4f)
            horizontalLineTo(8f)
            verticalLineTo(16f)
            close()
        }
    }.build()
}

private val rectangleAdd: ImageVector by lazy {
    ImageVector.Builder(
        name = "RectangleAdd",
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
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 18f)
            horizontalLineTo(20f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(18f)
            close()
            moveToRelative(0f, 0f)
            verticalLineTo(6f)
            verticalLineTo(18f)
            close()
            moveToRelative(7f, -2f)
            horizontalLineToRelative(2f)
            verticalLineTo(13f)
            horizontalLineToRelative(3f)
            verticalLineTo(11f)
            horizontalLineTo(13f)
            verticalLineTo(8f)
            horizontalLineTo(11f)
            verticalLineToRelative(3f)
            horizontalLineTo(8f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(3f)
            close()
        }
    }.build()
}

private val contentCopy: ImageVector by lazy {
    ImageVector.Builder(
        name = "ContentCopy",
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
            moveTo(9f, 18f)
            quadTo(8.18f, 18f, 7.59f, 17.41f)
            reflectiveQuadTo(7f, 16f)
            verticalLineTo(4f)
            quadTo(7f, 3.17f, 7.59f, 2.59f)
            reflectiveQuadTo(9f, 2f)
            horizontalLineToRelative(9f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(20f, 4f)
            verticalLineTo(16f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 18f)
            horizontalLineTo(9f)
            close()
            moveTo(9f, 16f)
            horizontalLineToRelative(9f)
            verticalLineTo(4f)
            horizontalLineTo(9f)
            verticalLineTo(16f)
            close()
            moveTo(5f, 22f)
            quadTo(4.18f, 22f, 3.59f, 21.41f)
            reflectiveQuadTo(3f, 20f)
            verticalLineTo(6f)
            horizontalLineTo(5f)
            verticalLineTo(20f)
            horizontalLineTo(16f)
            verticalLineToRelative(2f)
            horizontalLineTo(5f)
            close()
            moveTo(9f, 16f)
            verticalLineTo(4f)
            verticalLineTo(16f)
            close()
        }
    }.build()
}

private val expandContent: ImageVector by lazy {
    ImageVector.Builder(
        name = "ExpandContent",
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
            moveTo(5f, 19f)
            verticalLineTo(13f)
            horizontalLineTo(7f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(2f)
            horizontalLineTo(5f)
            close()
            moveTo(17f, 11f)
            verticalLineTo(7f)
            horizontalLineTo(13f)
            verticalLineTo(5f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(6f)
            horizontalLineTo(17f)
            close()
        }
    }.build()
}

private val markdown: ImageVector by lazy {
    ImageVector.Builder(
        name = "Markdown",
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
            moveTo(16f, 15f)
            lineToRelative(3f, -3f)
            lineTo(17.95f, 10.93f)
            lineToRelative(-1.2f, 1.2f)
            verticalLineTo(9f)
            horizontalLineToRelative(-1.5f)
            verticalLineToRelative(3.13f)
            lineToRelative(-1.2f, -1.2f)
            lineTo(13f, 12f)
            lineToRelative(3f, 3f)
            close()
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 18f)
            horizontalLineTo(20f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(18f)
            close()
            moveToRelative(0f, 0f)
            verticalLineTo(6f)
            verticalLineTo(18f)
            close()
            moveTo(5.5f, 15f)
            horizontalLineTo(7f)
            verticalLineTo(10.5f)
            horizontalLineTo(8f)
            verticalLineToRelative(3f)
            horizontalLineTo(9.5f)
            verticalLineToRelative(-3f)
            horizontalLineToRelative(1f)
            verticalLineTo(15f)
            horizontalLineTo(12f)
            verticalLineTo(10f)
            quadTo(12f, 9.57f, 11.71f, 9.29f)
            reflectiveQuadTo(11f, 9f)
            horizontalLineTo(6.5f)
            quadTo(6.08f, 9f, 5.79f, 9.29f)
            reflectiveQuadTo(5.5f, 10f)
            verticalLineToRelative(5f)
            close()
        }
    }.build()
}

private val rawOn: ImageVector by lazy {
    ImageVector.Builder(
        name = "RawOn",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(3f, 15f)
                verticalLineTo(9f)
                horizontalLineTo(6.5f)
                quadTo(7.1f, 9f, 7.55f, 9.45f)
                reflectiveQuadTo(8f, 10.5f)
                verticalLineToRelative(1f)
                quadToRelative(0f, 0.45f, -0.24f, 0.81f)
                reflectiveQuadTo(7.1f, 12.9f)
                lineTo(8f, 15f)
                horizontalLineTo(6.5f)
                lineTo(5.6f, 13f)
                horizontalLineTo(4.5f)
                verticalLineToRelative(2f)
                horizontalLineTo(3f)
                close()
                moveToRelative(5.75f, 0f)
                lineToRelative(1.5f, -6f)
                horizontalLineToRelative(2.5f)
                lineToRelative(1.5f, 6f)
                horizontalLineToRelative(-1.5f)
                lineTo(12.4f, 13.5f)
                horizontalLineTo(10.65f)
                lineTo(10.25f, 15f)
                horizontalLineTo(8.75f)
                close()
                moveToRelative(6.75f, 0f)
                lineTo(14f, 9f)
                horizontalLineToRelative(1.5f)
                lineToRelative(0.75f, 3f)
                lineTo(17f, 9f)
                horizontalLineToRelative(1.5f)
                lineToRelative(0.75f, 3f)
                lineTo(20f, 9f)
                horizontalLineToRelative(1.5f)
                lineTo(20f, 15f)
                horizontalLineTo(18.5f)
                lineTo(17.75f, 11.95f)
                lineTo(17f, 15f)
                horizontalLineTo(15.5f)
                close()
                moveTo(11f, 12f)
                horizontalLineToRelative(1f)
                lineTo(11.75f, 11f)
                horizontalLineToRelative(-0.5f)
                lineTo(11f, 12f)
                close()
                moveTo(4.5f, 11.5f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-1f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(1f)
                close()
            }
        }.build()
}

private val tune: ImageVector by lazy {
    ImageVector.Builder(
        name = "Tune",
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
            moveTo(11f, 21f)
            verticalLineTo(15f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(2f)
            horizontalLineTo(13f)
            verticalLineToRelative(2f)
            horizontalLineTo(11f)
            close()
            moveTo(3f, 19f)
            verticalLineTo(17f)
            horizontalLineTo(9f)
            verticalLineToRelative(2f)
            horizontalLineTo(3f)
            close()
            moveTo(7f, 15f)
            verticalLineTo(13f)
            horizontalLineTo(3f)
            verticalLineTo(11f)
            horizontalLineTo(7f)
            verticalLineTo(9f)
            horizontalLineTo(9f)
            verticalLineToRelative(6f)
            horizontalLineTo(7f)
            close()
            moveToRelative(4f, -2f)
            verticalLineTo(11f)
            horizontalLineTo(21f)
            verticalLineToRelative(2f)
            horizontalLineTo(11f)
            close()
            moveTo(15f, 9f)
            verticalLineTo(3f)
            horizontalLineToRelative(2f)
            verticalLineTo(5f)
            horizontalLineToRelative(4f)
            verticalLineTo(7f)
            horizontalLineTo(17f)
            verticalLineTo(9f)
            horizontalLineTo(15f)
            close()
            moveTo(3f, 7f)
            verticalLineTo(5f)
            horizontalLineTo(13f)
            verticalLineTo(7f)
            horizontalLineTo(3f)
            close()
        }
    }.build()
}

private val deleteForever: ImageVector by lazy {
    ImageVector.Builder(
        name = "DeleteForever",
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
            moveTo(9.4f, 16.5f)
            lineTo(12f, 13.9f)
            lineToRelative(2.6f, 2.6f)
            lineTo(16f, 15.1f)
            lineTo(13.4f, 12.5f)
            lineTo(16f, 9.9f)
            lineTo(14.6f, 8.5f)
            lineTo(12f, 11.1f)
            lineTo(9.4f, 8.5f)
            lineTo(8f, 9.9f)
            lineToRelative(2.6f, 2.6f)
            lineTo(8f, 15.1f)
            lineToRelative(1.4f, 1.4f)
            close()
            moveTo(7f, 21f)
            quadTo(6.18f, 21f, 5.59f, 20.41f)
            reflectiveQuadTo(5f, 19f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(4f)
            horizontalLineTo(9f)
            verticalLineTo(3f)
            horizontalLineToRelative(6f)
            verticalLineTo(4f)
            horizontalLineToRelative(5f)
            verticalLineTo(6f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(17f, 21f)
            horizontalLineTo(7f)
            close()
            moveTo(17f, 6f)
            horizontalLineTo(7f)
            verticalLineTo(19f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            close()
            moveTo(7f, 6f)
            verticalLineTo(19f)
            verticalLineTo(6f)
            close()
        }
    }.build()
}

private val collapseContent: ImageVector by lazy {
    ImageVector.Builder(
        name = "CollapseContent",
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
            moveTo(11f, 13f)
            verticalLineToRelative(6f)
            horizontalLineTo(9f)
            verticalLineTo(15f)
            horizontalLineTo(5f)
            verticalLineTo(13f)
            horizontalLineToRelative(6f)
            close()
            moveTo(15f, 5f)
            verticalLineTo(9f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(2f)
            horizontalLineTo(13f)
            verticalLineTo(5f)
            horizontalLineToRelative(2f)
            close()
        }
    }.build()
}
