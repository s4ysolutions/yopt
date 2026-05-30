package s4y.yopt.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ModelDef(
    val id: String,
    val providerId: String,
    val officialName: String,
    val enabled: Boolean = true
)
