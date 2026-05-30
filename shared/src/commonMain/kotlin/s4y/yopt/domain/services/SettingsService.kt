package s4y.yopt.domain.services

import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import s4y.yopt.domain.models.AuthCredentials
import s4y.yopt.domain.models.Chat
import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.services.AuthService

@Serializable
data class ExportData(
    val v: Int = 1,
    val models: List<ModelDef>,
    val chats: List<Chat>,
    val auth: List<AuthCredentials>,
    val globalInstructions: String = ""
)

class SettingsService(
    private val models: ModelService,
    private val chats: ChatService,
    private val authService: AuthService,
    private val prefs: AppPreferencesService
) {
    private val json = Json { prettyPrint = true }

    suspend fun export(): String {
        val data = ExportData(
            models = models.getAllModels(),
            chats = chats.observeAll().first(),
            auth = authService.observeCredentials().first(),
            globalInstructions = prefs.getGlobalInstructions()
        )
        return json.encodeToString(data)
    }

    suspend fun import(jsonString: String) {
        val data = Json.decodeFromString<ExportData>(jsonString)
        data.models.groupBy { it.providerId }.forEach { (pid, ms) ->
            models.upsertModels(pid, ms)
        }
        chats.importAll(data.chats)
        authService.importAll(data.auth)
        prefs.setGlobalInstructions(data.globalInstructions)
    }

    suspend fun importAppend(jsonString: String) {
        val data = Json.decodeFromString<ExportData>(jsonString)
        data.models.groupBy { it.providerId }.forEach { (pid, ms) ->
            models.mergeModels(pid, ms)
        }
        chats.mergeChats(data.chats)
        authService.mergeCredentials(data.auth)
        mergeGlobalInstructions(data.globalInstructions)
    }

    private suspend fun mergeGlobalInstructions(incoming: String) {
        val trimmed = incoming.trim()
        if (trimmed.isBlank()) return
        val existing = prefs.getGlobalInstructions()
        if (existing.isBlank() || existing == trimmed) {
            prefs.setGlobalInstructions(trimmed)
            return
        }
        if (existing.contains(trimmed) || trimmed.contains(existing)) return
        prefs.setGlobalInstructions("$existing\n$trimmed")
    }
}
