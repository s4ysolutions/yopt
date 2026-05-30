package s4y.yopt.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.Markdown

@Composable
fun MarkdownResponse(content: String, modifier: Modifier = Modifier) {
    Markdown(
        content = content,
        modifier = modifier,
        components = markdownComponents(
            table = { model ->
                MarkdownTable(
                    content = model.content,
                    node = model.node,
                    style = LocalMarkdownTypography.current.paragraph,
                    headerBlock = { content2, header, tableWidth, style ->
                        MarkdownTableHeader(
                            content = content2,
                            header = header,
                            tableWidth = tableWidth,
                            style = style,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip,
                        )
                    },
                    rowBlock = { content2, header, tableWidth, style ->
                        MarkdownTableRow(
                            content = content2,
                            header = header,
                            tableWidth = tableWidth,
                            style = style,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip,
                        )
                    },
                )
            }
        )
    )
}
