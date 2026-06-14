package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.services.ModelService

class ManageModelsUseCase(
    private val models: ModelService
) {
    suspend fun getEnabledModels(): List<ModelDef> = models.getEnabledModels()
    fun observeModels(): Flow<List<ModelDef>> = models.observeModels()
    fun observeEnabledModels(): Flow<List<ModelDef>> =
        models.observeModels().map { all -> all.filter { it.enabled } }
    suspend fun setModelEnabled(modelId: String, enabled: Boolean) {
        models.setModelEnabled(modelId, enabled)
    }
    suspend fun clearModels(providerId: String) {
        models.upsertModels(providerId, emptyList())
    }

    suspend fun setManualModel(providerId: String, modelName: String) {
        val id = "$providerId:$modelName"
        models.upsertModels(providerId, listOf(ModelDef(id, providerId, modelName)))
    }
}
