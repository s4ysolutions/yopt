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
