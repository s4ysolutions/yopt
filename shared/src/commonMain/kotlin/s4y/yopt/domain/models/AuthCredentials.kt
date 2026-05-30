package s4y.yopt.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthCredentials(
    val providerId: String,
    val apiKey: String? = null
)
