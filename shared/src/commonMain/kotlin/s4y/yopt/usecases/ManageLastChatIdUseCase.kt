package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.services.AppPreferencesService

class ManageLastChatIdUseCase(
    private val prefs: AppPreferencesService
) {
    fun observe(): Flow<String?> = prefs.observeLastChatId()
    suspend fun set(id: String?) = prefs.setLastChatId(id)
}
