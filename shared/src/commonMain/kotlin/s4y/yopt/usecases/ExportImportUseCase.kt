package s4y.yopt.usecases

import s4y.yopt.domain.services.SettingsService

class ExportImportUseCase(private val settings: SettingsService) {
    suspend fun export(): String = settings.export()
    suspend fun import(json: String) = settings.import(json)
    suspend fun importAppend(json: String) = settings.importAppend(json)
}
