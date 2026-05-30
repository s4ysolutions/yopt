package s4y.yopt.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import yopt.composeapp.generated.resources.Res
import yopt.composeapp.generated.resources.save_to_file
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun SaveFileButton(onContent: suspend () -> String) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val content = onContent()
            val dialog = FileDialog(null as Frame?, "Save settings", FileDialog.SAVE)
            dialog.file = "yopt-settings.json"
            dialog.isVisible = true
            val dir = dialog.directory
            val name = dialog.file
            if (dir != null && name != null) {
                withContext(Dispatchers.IO) {
                    File(dir, name).writeText(content)
                }
            }
        }
    }) { Text(stringResource(Res.string.save_to_file)) }
}

@Composable
actual fun OpenFileButton(label: String, onResult: suspend (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val dialog = FileDialog(null as Frame?, "Import settings", FileDialog.LOAD)
            dialog.file = "*.json"
            dialog.isVisible = true
            val content = if (dialog.file != null) {
                withContext(Dispatchers.IO) {
                    File(dialog.directory, dialog.file).readText()
                }
            } else null
            onResult(content)
        }
    }) { Text(label) }
}
