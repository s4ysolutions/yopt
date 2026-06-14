package s4y.yopt.usecases

import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.models.ProviderDef
import s4y.yopt.domain.services.LLMService
import s4y.yopt.domain.services.ModelService

class RefreshModelsUseCase(
    private val models: ModelService,
    private val llm: LLMService
) {
    suspend fun refresh(provider: ProviderDef, apiKey: String?): List<ModelDef> {
        val fetched = llm.fetchModels(provider, apiKey)
        models.upsertModels(provider.id, fetched)
        return fetched
    }

    /** Never throws. Returns null on success, error message on failure. */
    suspend fun refreshOrError(provider: ProviderDef, apiKey: String?): String? {
        return try {
            val fetched = llm.fetchModels(provider, apiKey)
            models.upsertModels(provider.id, fetched)
            null
        } catch (e: Exception) {
            e.message ?: "Failed to refresh models for ${provider.name}"
        }
    }
}
