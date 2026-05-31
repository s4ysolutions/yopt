import Foundation
import ComposeApp

/// Wraps AppModule (from KMP) and provides typed access to all use cases.
/// Imports `ComposeApp` which transitively includes `YoPtShared`.
@MainActor
final class KotlinBridge: ObservableObject {
    static let shared = KotlinBridge()

    let module: AppModule

    private init() {
        // Create AppModule. On Apple platforms, platformContext is nil
        // (KeyValueStore/SecureStore use NSUserDefaults/Keychain respectively).
        self.module = AppModule(platformContext: nil)
    }

    // MARK: - Use Case Accessors

    var chatsUseCase: ManageChatsUseCase { module.chatsUseCase }
    var modelsUseCase: ManageModelsUseCase { module.modelsUseCase }
    var modelSelectionUseCase: ManageModelSelectionUseCase { module.modelSelectionUseCase }
    var sendUseCase: SendPromptUseCase { module.sendUseCase }
    var manageAuthUseCase: ManageAuthUseCase { module.manageAuthUseCase }
    var refreshModelsUseCase: RefreshModelsUseCase { module.refreshModelsUseCase }
    var exportUseCase: ExportImportUseCase { module.exportUseCase }
    var manageProvidersUseCase: ManageProvidersUseCase { module.manageProvidersUseCase }
    var responseDisplayUseCase: ManageResponseDisplayUseCase { module.responseDisplayUseCase }
    var lastChatIdUseCase: ManageLastChatIdUseCase { module.lastChatIdUseCase }
    var lastPromptUseCase: ManageLastPromptUseCase { module.lastPromptUseCase }
    var splitFractionUseCase: ManageSplitFractionUseCase { module.splitFractionUseCase }
    var globalInstructionsUseCase: ManageGlobalInstructionsUseCase { module.globalInstructionsUseCase }
}
