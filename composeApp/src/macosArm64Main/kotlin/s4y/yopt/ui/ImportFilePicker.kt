package s4y.yopt.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AppKit.NSOpenPanel
import platform.AppKit.NSSavePanel
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fprintf

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SaveFileButton(onContent: suspend () -> String) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val content = onContent()
            val panel = NSSavePanel()
            panel.nameFieldStringValue = "yopt-settings.json"
            if (panel.runModal() == 1L) {
                panel.URL?.path?.let { path ->
                    withContext(Dispatchers.Default) {
                        val fp = fopen(path, "w") ?: return@withContext
                        try {
                            fprintf(fp, "%s", content)
                        } finally {
                            fclose(fp)
                        }
                    }
                }
            }
        }
    }) { Text("Save to file") }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun OpenFileButton(label: String, onResult: suspend (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val panel = NSOpenPanel.openPanel()
            panel.canChooseFiles = true
            panel.canChooseDirectories = false
            val content = if (panel.runModal() == 1L) {
                panel.URL?.path?.let { path ->
                    withContext(Dispatchers.Default) {
                        readAllText(path)
                    }
                }
            } else null
            onResult(content)
        }
    }) { Text(label) }
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
private fun readAllText(path: String): String? {
    val data = NSData.create(contentsOfFile = path) ?: return null
    return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
}
