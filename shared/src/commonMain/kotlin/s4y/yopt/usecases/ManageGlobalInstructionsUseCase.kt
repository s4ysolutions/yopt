package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.services.AppPreferencesService

class ManageGlobalInstructionsUseCase(
    private val prefs: AppPreferencesService
) {
    fun observe(): Flow<String> = prefs.observeGlobalInstructions()
    fun get(): String = prefs.getGlobalInstructions()
    suspend fun set(value: String) = prefs.setGlobalInstructions(value)
}
