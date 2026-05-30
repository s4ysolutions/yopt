package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.services.AppPreferencesService

class ManageResponseDisplayUseCase(
    private val prefs: AppPreferencesService
) {
    fun observeDefaultShowMarkdown(): Flow<Boolean> = prefs.observeDefaultShowMarkdown()
    suspend fun setDefaultShowMarkdown(show: Boolean) = prefs.setDefaultShowMarkdown(show)
}
