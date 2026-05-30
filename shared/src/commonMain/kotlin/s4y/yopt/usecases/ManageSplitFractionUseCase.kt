package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.services.AppPreferencesService

class ManageSplitFractionUseCase(
    private val prefs: AppPreferencesService
) {
    fun observe(): Flow<Float> = prefs.observeSplitFraction()
    suspend fun set(value: Float) = prefs.setSplitFraction(value)
}
