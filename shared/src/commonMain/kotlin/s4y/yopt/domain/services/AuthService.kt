package s4y.yopt.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import s4y.yopt.domain.models.AuthCredentials
import s4y.yopt.domain.ports.SecureStore

class AuthService(
    private val store: SecureStore
) {
    private val _credentials = MutableStateFlow(load())

    fun observeCredentials(): Flow<List<AuthCredentials>> = _credentials

    suspend fun getCredentials(providerId: String): AuthCredentials? =
        _credentials.value.find { it.providerId == providerId }

    suspend fun saveApiKey(providerId: String, apiKey: String) {
        val existing = getCredentials(providerId)
        upsert(existing?.copy(apiKey = apiKey) ?: AuthCredentials(providerId, apiKey = apiKey))
    }

    suspend fun deleteCredentials(providerId: String) {
        _credentials.value = _credentials.value.filter { it.providerId != providerId }
        save()
    }

    suspend fun importAll(credentials: List<AuthCredentials>) {
        _credentials.value = credentials
        save()
    }

    suspend fun mergeCredentials(credentials: List<AuthCredentials>) {
        val seen = _credentials.value.map { it.providerId }.toMutableSet()
        for (c in credentials) {
            if (c.providerId !in seen) {
                upsert(c)
                seen += c.providerId
            }
        }
    }

    private fun upsert(c: AuthCredentials) {
        _credentials.value = _credentials.value.filter { it.providerId != c.providerId } + c
        save()
    }

    private fun load(): List<AuthCredentials> {
        return try {
            store.getString("auth")?.let { Json.decodeFromString(it) } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun save() {
        store.putString("auth", Json.encodeToString(_credentials.value))
    }
}
