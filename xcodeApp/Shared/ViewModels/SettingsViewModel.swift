import Foundation
import ComposeApp

@MainActor
final class SettingsViewModel: ObservableObject {
    private let bridge = KotlinBridge.shared

    @Published var models: [ModelDefModel] = []
    @Published var chats: [ChatModel] = []
    @Published var creds: [AuthCredentialsModel] = []
    @Published var providers: [ProviderModel] = []
    @Published var globalInstructions: String = ""

    @Published var exportContent: String? = nil
    @Published var exportError: String? = nil
    @Published var importReplaceError: String? = nil
    @Published var importAppendError: String? = nil
    @Published var dialogTitle: String? = nil
    @Published var dialogText: String? = nil
    @Published var refreshError: String? = nil

    @Published var selectedTab: Int = 0

    private var observationTasks: [Task<Void, Never>] = []

    init() {
        observeFlows()
    }

    deinit { observationTasks.forEach { $0.cancel() } }

    private func observeFlows() {
        observationTasks.append(Task {
            for await ms in bridge.modelsUseCase.observeModels() {
                self.models = ms.map(ModelDefModel.fromKotlin)
            }
        })
        observationTasks.append(Task {
            for await cs in bridge.chatsUseCase.observeAll() {
                self.chats = cs.map(ChatModel.fromKotlin)
            }
        })
        observationTasks.append(Task {
            for await cs in bridge.manageAuthUseCase.observeCredentials() {
                self.creds = cs.map(AuthCredentialsModel.fromKotlin)
            }
        })
        observationTasks.append(Task {
            for await ps in bridge.manageProvidersUseCase.observeProviders() {
                self.providers = ps.map(ProviderModel.fromKotlin)
            }
        })
        observationTasks.append(Task {
            for await instr in bridge.globalInstructionsUseCase.observe() {
                self.globalInstructions = instr
            }
        })
    }

    // Provider actions
    func saveApiKey(providerId: String, key: String) {
        Task { try? await bridge.manageAuthUseCase.saveApiKey(providerId: providerId, apiKey: key) }
    }

    func clearCredentials(providerId: String) {
        Task { try? await bridge.manageAuthUseCase.deleteCredentials(providerId: providerId) }
    }

    func refreshModels(provider: ProviderModel, apiKey: String?) {
        Task {
            let kp = provider.toKotlinProvider()
            do {
                let raw = try await bridge.refreshModelsUseCase.refresh(provider: kp, apiKey: apiKey)
                _ = try resultOrThrow(result: raw)
            } catch {
                self.refreshError = error.localizedDescription
            }
        }
    }

    func setManualModel(providerId: String, modelName: String) {
        Task { try? await bridge.modelsUseCase.setManualModel(providerId: providerId, modelName: modelName) }
    }

    func toggleModelEnabled(_ modelId: String) {
        let model = models.first { $0.id == modelId }
        Task { try? await bridge.modelsUseCase.setModelEnabled(modelId: modelId, enabled: !(model?.enabled ?? false)) }
    }

    func addCustomProvider(name: String, apiStyle: ApiStyleModel, baseUrl: String) {
        Task { _ = try? await bridge.manageProvidersUseCase.addCustomProvider(name: name, apiStyle: apiStyle.toKotlin(), baseUrl: baseUrl) }
    }

    func updateCustomProvider(_ provider: ProviderModel, name: String, baseUrl: String, apiStyle: ApiStyleModel) {
        let updated = ProviderModel(id: provider.id, name: name, apiStyle: apiStyle, baseUrl: baseUrl, predefined: provider.predefined)
        Task { _ = try? await bridge.manageProvidersUseCase.updateCustomProvider(def: updated.toKotlinProvider()) }
    }

    func deleteCustomProvider(_ id: String) {
        Task { try? await bridge.manageProvidersUseCase.deleteCustomProvider(id: id) }
    }

    // Chat actions
    func updateChat(_ chat: ChatModel, title: String, instructions: String, labels: [String]) {
        Task { _ = try? await bridge.chatsUseCase.update(chat: chat.toKotlinChat()) }
    }

    func deleteChat(_ id: String) {
        Task { try? await bridge.chatsUseCase.delete(chatId: id) }
    }

    // Global instructions
    func setGlobalInstructions(_ text: String) {
        Task { try? await bridge.globalInstructionsUseCase.set(value: text) }
    }

    // Export/Import
    func export() {
        Task {
            do {
                let json = try await bridge.exportUseCase.export()
                self.exportContent = json
            } catch {
                self.exportError = error.localizedDescription
            }
        }
    }

    func importReplace(json: String) {
        Task {
            do {
                let result = try await bridge.exportUseCase.import(json: json)
                self.dialogTitle = String(localized: "import.title")
                self.dialogText = String(format: String(localized: "import.replaceMessage"), Int(result.chats), Int(result.providers))
            } catch {
                self.importReplaceError = error.localizedDescription
            }
        }
    }

    func importAppend(json: String) {
        Task {
            do {
                let result = try await bridge.exportUseCase.importAppend(json: json)
                self.dialogTitle = String(localized: "import.appendTitle")
                self.dialogText = String(format: String(localized: "import.appendMessage"), Int(result.chats), Int(result.providers))
            } catch {
                self.importAppendError = error.localizedDescription
            }
        }
    }
}
