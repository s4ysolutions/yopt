package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.models.ApiStyle
import s4y.yopt.domain.models.AuthType
import s4y.yopt.domain.models.ProviderDef
import s4y.yopt.domain.services.AuthService
import s4y.yopt.domain.services.ModelService

class ManageProvidersUseCase(
    private val models: ModelService,
    private val auth: AuthService
) {
    fun observeProviders(): Flow<List<ProviderDef>> = models.observeProviders()

    suspend fun addCustomProvider(name: String, apiStyle: ApiStyle, baseUrl: String): ProviderDef {
        val existing = models.getProviders().filter { !it.predefined }
        var next = 1
        while (existing.any { it.id == "custom-$next" }) next++
        val def = ProviderDef(
            id = "custom-$next",
            name = name,
            apiStyle = apiStyle,
            authType = AuthType.ApiKey("$name API Key"),
            baseUrl = baseUrl,
            predefined = false
        )
        models.addCustomProvider(def)
        return def
    }

    suspend fun updateCustomProvider(def: ProviderDef) {
        models.updateCustomProvider(def)
    }

    suspend fun deleteCustomProvider(id: String) {
        auth.deleteCredentials(id)
        models.deleteCustomProvider(id)
    }
}
