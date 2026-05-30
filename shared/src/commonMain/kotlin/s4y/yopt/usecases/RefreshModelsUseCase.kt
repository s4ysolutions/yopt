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
}
