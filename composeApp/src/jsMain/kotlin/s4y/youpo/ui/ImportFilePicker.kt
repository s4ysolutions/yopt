package s4y.yopt.ui

import androidx.compose.runtime.Composable

@Composable
actual fun SaveFileButton(onContent: suspend () -> String) {}
@Composable
actual fun OpenFileButton(label: String, onResult: suspend (String?) -> Unit) {}
