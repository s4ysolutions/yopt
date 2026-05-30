package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.models.AuthCredentials
import s4y.yopt.domain.services.AuthService

class ManageAuthUseCase(private val authService: AuthService) {
    fun observeCredentials(): Flow<List<AuthCredentials>> =
        authService.observeCredentials()

    suspend fun saveApiKey(providerId: String, apiKey: String) {
        authService.saveApiKey(providerId, apiKey)
    }

    suspend fun deleteCredentials(providerId: String) {
        authService.deleteCredentials(providerId)
    }
}
