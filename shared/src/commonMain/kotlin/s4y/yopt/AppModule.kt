package s4y.yopt

import s4y.yopt.domain.services.AppPreferencesService
import s4y.yopt.domain.services.ChatService
import s4y.yopt.domain.services.ModelService
import s4y.yopt.domain.services.SettingsService
import s4y.yopt.domain.ports.KeyValueStore
import s4y.yopt.adapters.KtorHttpAdapter
import s4y.yopt.domain.services.LLMService
import s4y.yopt.domain.ports.SecureStore
import s4y.yopt.domain.services.AuthService
import s4y.yopt.usecases.ExportImportUseCase
import s4y.yopt.usecases.ManageAuthUseCase
import s4y.yopt.usecases.ManageChatsUseCase
import s4y.yopt.usecases.ManageGlobalInstructionsUseCase
import s4y.yopt.usecases.ManageLastChatIdUseCase
import s4y.yopt.usecases.ManageLastPromptUseCase
import s4y.yopt.usecases.ManageModelSelectionUseCase
import s4y.yopt.usecases.ManageProvidersUseCase
import s4y.yopt.usecases.ManageModelsUseCase
import s4y.yopt.usecases.ManageResponseDisplayUseCase
import s4y.yopt.usecases.RefreshModelsUseCase
import s4y.yopt.usecases.ManageSplitFractionUseCase
import s4y.yopt.usecases.SendPromptUseCase

class AppModule(platformContext: Any? = null) {
    private val kv = KeyValueStore(platformContext)
    private val secureStore = SecureStore(platformContext)
    private val models = ModelService(kv)
    private val chats = ChatService(kv)
    private val authService = AuthService(secureStore)
    private val llm = LLMService(models, KtorHttpAdapter())
    private val prefsService = AppPreferencesService(kv)

    val modelsUseCase = ManageModelsUseCase(models)
    val modelSelectionUseCase = ManageModelSelectionUseCase(prefsService)
    val chatsUseCase = ManageChatsUseCase(chats)
    val sendUseCase = SendPromptUseCase(
        llm, models, authService, chats, prefsService
    )
    val exportUseCase = ExportImportUseCase(
        SettingsService(models, chats, authService, prefsService)
    )
    val manageAuthUseCase = ManageAuthUseCase(authService)
    val refreshModelsUseCase = RefreshModelsUseCase(models, llm)
    val responseDisplayUseCase = ManageResponseDisplayUseCase(prefsService)
    val lastChatIdUseCase = ManageLastChatIdUseCase(prefsService)
    val globalInstructionsUseCase = ManageGlobalInstructionsUseCase(prefsService)
    val lastPromptUseCase = ManageLastPromptUseCase(prefsService)
    val splitFractionUseCase = ManageSplitFractionUseCase(prefsService)
    val manageProvidersUseCase = ManageProvidersUseCase(models, authService)
    val modelProviders get() = models.getProviders()
}
