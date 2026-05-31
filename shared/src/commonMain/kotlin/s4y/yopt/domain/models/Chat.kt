package s4y.yopt.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String,
    val title: String,
    val instructions: String = "",
    val defaultModelId: String? = null,
    val labels: List<String> = emptyList(),
    val expandedTimestamps: Set<Long> = emptySet(),
    val history: List<ResponseEntry> = emptyList()
)
