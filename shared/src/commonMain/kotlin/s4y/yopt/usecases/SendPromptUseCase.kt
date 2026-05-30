package s4y.yopt.usecases

import s4y.yopt.domain.models.Chat
import s4y.yopt.domain.models.ResponseEntry
import s4y.yopt.domain.services.AppPreferencesService
import s4y.yopt.domain.services.ChatService
import s4y.yopt.domain.services.AuthService
import s4y.yopt.domain.services.LLMService
import s4y.yopt.domain.services.ModelService

class SendPromptUseCase(
    private val llm: LLMService,
    private val models: ModelService,
    private val authService: AuthService,
    private val chats: ChatService,
    private val prefs: AppPreferencesService,
) {
    suspend operator fun invoke(chat: Chat, prompt: String, modelId: String?): Result<ResponseEntry> {
        return try {
            val model = models.getEnabledModels()
                .find { it.id == (modelId ?: chat.defaultModelId) }
                ?: return Result.failure(Exception("No enabled model selected"))
            val credentials = authService.getCredentials(model.providerId)
            val startedAt = currentTimeMillis()
            val sys = listOfNotNull(
                prefs.getGlobalInstructions().takeIf { it.isNotBlank() },
                chat.instructions.takeIf { it.isNotBlank() }
            ).joinToString("\n").ifBlank { null }
            val response = llm.send(
                prompt, model, sys,
                credentials?.apiKey
            )
            val endedAt = currentTimeMillis()
            val entry = ResponseEntry(
                timestamp = startedAt,
                prompt = prompt,
                response = response,
                modelId = model.id,
                modelName = model.officialName,
                durationMs = endedAt - startedAt,
                showMarkdown = prefs.getDefaultShowMarkdown()
            )
            chats.appendHistory(chat.id, entry)
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

expect fun currentTimeMillis(): Long
