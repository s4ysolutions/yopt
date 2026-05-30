package s4y.yopt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import s4y.yopt.ui.MainScreen
import s4y.yopt.ui.YoPtTheme

@Composable
fun App(platformContext: Any? = null) {
    val module = remember { AppModule(platformContext) }

    YoPtTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            MainScreen(
                sendUseCase = module.sendUseCase,
                modelsUseCase = module.modelsUseCase,
                modelSelectionUseCase = module.modelSelectionUseCase,
                chatsUseCase = module.chatsUseCase,
                exportUseCase = module.exportUseCase,
                manageAuthUseCase = module.manageAuthUseCase,
                refreshModelsUseCase = module.refreshModelsUseCase,
                manageProvidersUseCase = module.manageProvidersUseCase,
                responseDisplayUseCase = module.responseDisplayUseCase,
                lastChatIdUseCase = module.lastChatIdUseCase,
                lastPromptUseCase = module.lastPromptUseCase,
                splitFractionUseCase = module.splitFractionUseCase,
                globalInstructionsUseCase = module.globalInstructionsUseCase
            )
        }
    }
}
