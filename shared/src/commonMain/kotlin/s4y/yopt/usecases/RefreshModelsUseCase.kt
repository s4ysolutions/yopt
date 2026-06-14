package s4y.yopt.usecases

import kotlinx.coroutines.CancellationException
import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.models.ProviderDef
import s4y.yopt.domain.services.LLMService
import s4y.yopt.domain.services.ModelService

class RefreshModelsUseCase(
    private val models: ModelService,
    private val llm: LLMService
) {
    suspend fun refresh(provider: ProviderDef, apiKey: String?): Result<List<ModelDef>> =
        try {
            val fetched = llm.fetchModels(provider, apiKey)
            models.upsertModels(provider.id, fetched)
            Result.success(fetched)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
