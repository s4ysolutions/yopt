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

    @Published var exportError: String? = nil
    @Published var importReplaceError: String? = nil
    @Published var importAppendError: String? = nil
    @Published var dialogTitle: String? = nil
    @Published var dialogText: String? = nil

    private var observationTasks: [Task<Void, Never>] = []

    init() {
        observeFlows()
    }

    deinit { observationTasks.forEach { $0.cancel() } }

    private func observeFlows() {
        observationTasks.append(Task {
            do {
                for try await ms in bridge.modelsUseCase.observeModels() {
                    self.models = (ms as? [ModelDef] ?? []).map(ModelDefModel.fromKotlin)
                }
            } catch {}
        })
        observationTasks.append(Task {
            do {
                for try await cs in bridge.chatsUseCase.observeAll() {
                    self.chats = (cs as? [Chat] ?? []).map(ChatModel.fromKotlin)
                }
            } catch {}
        })
        observationTasks.append(Task {
            do {
                for try await cs in bridge.manageAuthUseCase.observeCredentials() {
                    self.creds = (cs as? [AuthCredentials] ?? []).map(AuthCredentialsModel.fromKotlin)
                }
            } catch {}
        })
        observationTasks.append(Task {
            do {
                for try await ps in bridge.manageProvidersUseCase.observeProviders() {
                    self.providers = (ps as? [ProviderDef] ?? []).map(ProviderModel.fromKotlin)
                }
            } catch {}
        })
        observationTasks.append(Task {
            do {
                for try await instr in bridge.globalInstructionsUseCase.observe() {
                    self.globalInstructions = instr as? String ?? ""
                }
            } catch {}
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
            let kotlinProvider = provider.toKotlinProvider()
            _ = try? await bridge.refreshModelsUseCase.refresh(provider: kotlinProvider, apiKey: apiKey)
        }
    }

    func toggleModelEnabled(_ modelId: String) {
        let model = models.first { $0.id == modelId }
        Task { try? await bridge.modelsUseCase.setModelEnabled(modelId: modelId, enabled: !(model?.enabled ?? false)) }
    }

    func addCustomProvider(name: String, apiStyle: ApiStyleModel, baseUrl: String) {
        Task { _ = try? await bridge.manageProvidersUseCase.addCustomProvider(name: name, apiStyle: apiStyle.toKotlin(), baseUrl: baseUrl) }
    }

    func updateCustomProvider(_ provider: ProviderModel, name: String, baseUrl: String, apiStyle: ApiStyleModel) {
        Task { _ = try? await bridge.manageProvidersUseCase.updateCustomProvider(def: provider.toKotlinProvider()) }
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
                let json = try await bridge.exportUseCase.export() as! String
                self.dialogTitle = "Export"
                self.dialogText = "Data exported (\(json.count) chars). Use Save panel to write."
            } catch {
                self.exportError = error.localizedDescription
            }
        }
    }

    func importReplace(json: String) {
        Task {
            do {
                try await bridge.exportUseCase.import(json: json)
                self.dialogTitle = "Import"
                self.dialogText = "Settings replaced successfully."
            } catch {
                self.importReplaceError = error.localizedDescription
            }
        }
    }

    func importAppend(json: String) {
        Task {
            do {
                try await bridge.exportUseCase.importAppend(json: json)
                self.dialogTitle = "Import Append"
                self.dialogText = "Settings appended successfully."
            } catch {
                self.importAppendError = error.localizedDescription
            }
        }
    }
}
