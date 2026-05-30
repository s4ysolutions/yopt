package s4y.yopt.domain.models

import kotlinx.serialization.Serializable

@Serializable
sealed class AuthType {
    @Serializable
    data class ApiKey(val keyName: String) : AuthType()
}
