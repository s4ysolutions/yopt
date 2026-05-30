package s4y.yopt.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ResponseEntry(
    val timestamp: Long,
    val prompt: String,
    val response: String,
    val modelId: String,
    val modelName: String,
    val durationMs: Long = 0,
    val showMarkdown: Boolean = false
)
