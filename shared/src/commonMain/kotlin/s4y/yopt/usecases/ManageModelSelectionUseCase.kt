package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.services.AppPreferencesService

class ManageModelSelectionUseCase(
    private val prefs: AppPreferencesService
) {
    fun observe(): Flow<String?> = prefs.observeSelectedModel()
    suspend fun set(modelId: String) = prefs.setSelectedModel(modelId)
}
