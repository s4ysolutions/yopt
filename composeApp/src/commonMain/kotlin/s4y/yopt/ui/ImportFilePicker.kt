package s4y.yopt.ui

import androidx.compose.runtime.Composable

@Composable
expect fun SaveFileButton(onContent: suspend () -> String)

@Composable
expect fun OpenFileButton(label: String = "Load from file", onResult: suspend (String?) -> Unit)
