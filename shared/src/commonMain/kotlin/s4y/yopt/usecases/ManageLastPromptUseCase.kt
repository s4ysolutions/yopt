package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.services.AppPreferencesService

class ManageLastPromptUseCase(
    private val prefs: AppPreferencesService
) {
    fun observe(): Flow<String> = prefs.observeLastPrompt()
    suspend fun set(value: String) = prefs.setLastPrompt(value)
}
