import SwiftUI

struct ProvidersTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel
    @State private var expandedProviderId: String? = nil
    @State private var showAddCustom = false

    var body: some View {
        List {
            ForEach(settingsVM.providers) { provider in
                let cred = settingsVM.creds.first { $0.providerId == provider.id }
                let isExpanded = expandedProviderId == provider.id
                ProviderCardView(
                    provider: provider,
                    credential: cred,
                    isExpanded: isExpanded,
                    models: settingsVM.models.filter { $0.providerId == provider.id },
                    onToggle: { expandedProviderId = isExpanded ? nil : provider.id },
                    onSaveApiKey: { settingsVM.saveApiKey(providerId: provider.id, key: $0) },
                    onClearCredentials: { settingsVM.clearCredentials(providerId: provider.id) },
                    onRefresh: { settingsVM.refreshModels(provider: provider, apiKey: cred?.apiKey) },
                    onToggleModel: settingsVM.toggleModelEnabled,
                    onUpdateCustom: { name, url, style in
                        settingsVM.updateCustomProvider(provider, name: name, baseUrl: url, apiStyle: style)
                    },
                    onDeleteCustom: { settingsVM.deleteCustomProvider(provider.id) }
                )
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(top: DesignTokens.padding4, leading: 0, bottom: DesignTokens.padding4, trailing: 0))
            }

            Button(action: { showAddCustom = true }) {
                Label(String(localized: "providers.addCustom"), systemImage: "plus.circle")
            }
            .listRowSeparator(.hidden)
            .popover(isPresented: $showAddCustom) {
                AddCustomProviderView(settingsVM: settingsVM, isPresented: $showAddCustom)
            }
        }
        .listStyle(.plain)
        .alert(String(localized: "providers.refreshError.title"), isPresented: Binding(
            get: { settingsVM.refreshError != nil },
            set: { if !$0 { settingsVM.refreshError = nil } }
        )) {
            Button(String(localized: "button.ok"), role: .cancel) { settingsVM.refreshError = nil }
        } message: {
            Text(settingsVM.refreshError ?? "")
        }
    }
}

struct ProviderCardView: View {
    let provider: ProviderModel
    let credential: AuthCredentialsModel?
    let isExpanded: Bool
    let models: [ModelDefModel]
    let onToggle: () -> Void
    let onSaveApiKey: (String) -> Void
    let onClearCredentials: () -> Void
    let onRefresh: () -> Void
    let onToggleModel: (String) -> Void
    let onUpdateCustom: (String, String, ApiStyleModel) -> Void
    let onDeleteCustom: () -> Void

    var hasKey: Bool { !(credential?.apiKey ?? "").isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading) {
                    Text(provider.name)
                        .font(.headline)
                    Text(hasKey ? String(localized: "providers.apiKeySet") : String(localized: "providers.notConfigured"))
                        .font(.body)
                        .foregroundColor(hasKey ? Color.accentColor : Color.red)
                }
                Spacer()

                if isExpanded {
                    Button(action: onRefresh) {
                        Image(systemName: "arrow.clockwise")
                            .actionIcon()
                    }
                    .buttonStyle(.plain)
                    .disabled(!hasKey)
                    .help(String(localized: "help.refreshModels"))
                }

                Button(action: onToggle) {
                    Image(systemName: isExpanded ? "chevron.up.circle" : "chevron.down.circle")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help(isExpanded ? String(localized: "tooltip.collapse") : String(localized: "tooltip.expand"))
            }

            if isExpanded {
                if !provider.predefined {
                    CustomProviderEditView(
                        provider: provider,
                        credential: credential,
                        onSave: onSaveApiKey,
                        onUpdate: onUpdateCustom,
                        onDelete: onDeleteCustom
                    )
                } else {
                    ApiKeyEditView(
                        provider: provider,
                        credential: credential,
                        hasKey: hasKey,
                        onSave: onSaveApiKey,
                        onClear: onClearCredentials
                    )
                }

                if !models.isEmpty {
                    ModelListView(
                        models: models,
                        onToggle: onToggleModel
                    )
                }
            }
        }
        .padding(8)
        .background(Color.secondary.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

struct ApiKeyEditView: View {
    let provider: ProviderModel
    let credential: AuthCredentialsModel?
    let hasKey: Bool
    let onSave: (String) -> Void
    let onClear: () -> Void

    @State private var editing = false
    @State private var key = ""

    var body: some View {
        if hasKey && !editing {
            HStack {
                Button(String(localized: "button.change")) { editing = true }
                Button(String(localized: "button.clear"), role: .destructive, action: onClear)
            }
        } else {
            VStack(alignment: .leading, spacing: 8) {
                TextField(hasKey ? String(localized: "providers.newApiKeyPlaceholder") : provider.name + " API Key", text: $key)
                    .textFieldStyle(.roundedBorder)

                if !hasKey {
                    let url = providerUrl(for: provider.id)
                    if let url = url {
                        Link(String(localized: "providers.getApiKey"), destination: url)
                            .font(.body)
                    }
                }

                HStack(spacing: 8) {
                    Button(String(localized: "button.save")) {
                        onSave(key)
                        key = ""
                        editing = false
                    }
                    .buttonStyle(.borderedProminent)

                    if hasKey {
                        Button(String(localized: "button.cancel")) {
                            key = ""
                            editing = false
                        }
                    }
                }
            }
        }
    }

    private func providerUrl(for id: String) -> URL? {
        switch id {
        case "openai": return URL(string: "https://platform.openai.com/api-keys")
        case "anthropic": return URL(string: "https://console.anthropic.com/settings/keys")
        case "google": return URL(string: "https://aistudio.google.com/apikey")
        case "openrouter": return URL(string: "https://openrouter.ai/keys")
        case "deepseek": return URL(string: "https://platform.deepseek.com/api_keys")
        case "qwen": return URL(string: "https://bailian.console.aliyun.com/?apiKey=1")
        case "huggingface": return URL(string: "https://huggingface.co/settings/tokens")
        case "xai": return URL(string: "https://console.x.ai/")
        default: return nil
        }
    }
}

struct CustomProviderEditView: View {
    let provider: ProviderModel
    let credential: AuthCredentialsModel?
    let onSave: (String) -> Void
    let onUpdate: (String, String, ApiStyleModel) -> Void
    let onDelete: () -> Void

    @State private var editName: String = ""
    @State private var editBaseUrl: String = ""
    @State private var editApiStyle: ApiStyleModel = .openai
    @State private var editApiKey: String = ""
    @State private var styleExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            TextField(String(localized: "providers.providerNamePlaceholder"), text: $editName)
                .textFieldStyle(.roundedBorder)
            TextField(String(localized: "providers.baseUrlPlaceholder"), text: $editBaseUrl)
                .textFieldStyle(.roundedBorder)

            HStack {
                Text("API Style: \(editApiStyle.rawValue)")
                Spacer()
                Button(String(localized: "button.change")) { styleExpanded.toggle() }
                    .popover(isPresented: $styleExpanded) {
                        Picker("API Style", selection: $editApiStyle) {
                            ForEach(ApiStyleModel.allCases, id: \.self) { style in
                                Text(style.rawValue).tag(style)
                            }
                        }
                        #if os(macOS)
                        .pickerStyle(.radioGroup)
                        #else
                        .pickerStyle(.wheel)
                        #endif
                        .padding()
                    }
            }

            TextField(String(localized: "providers.apiKeyPlaceholder"), text: $editApiKey)
                .textFieldStyle(.roundedBorder)

            HStack {
                Button(String(localized: "button.save")) {
                    onUpdate(editName, editBaseUrl, editApiStyle)
                    if !editApiKey.isEmpty {
                        onSave(editApiKey)
                    }
                }
                .buttonStyle(.borderedProminent)

                Button(String(localized: "button.delete"), role: .destructive, action: onDelete)
            }
        }
        .onAppear {
            editName = provider.name
            editBaseUrl = provider.baseUrl
            editApiStyle = provider.apiStyle
            editApiKey = credential?.apiKey ?? ""
        }
    }
}

struct AddCustomProviderView: View {
    @ObservedObject var settingsVM: SettingsViewModel
    @Binding var isPresented: Bool

    @State private var newName = ""
    @State private var newBaseUrl = ""
    @State private var newApiStyle: ApiStyleModel = .openai
    @State private var newApiKey = ""
    @State private var styleExpanded = false

    var body: some View {
        VStack(spacing: 12) {
            Text(String(localized: "providers.addCustom")).font(.headline)
            TextField(String(localized: "providers.providerNamePlaceholder"), text: $newName).textFieldStyle(.roundedBorder)
            TextField(String(localized: "providers.baseUrlPlaceholder"), text: $newBaseUrl).textFieldStyle(.roundedBorder)

            HStack {
                Text("API Style: \(newApiStyle.rawValue)")
                Spacer()
                Button(String(localized: "button.change")) { styleExpanded.toggle() }
                    .popover(isPresented: $styleExpanded) {
                        Picker("API Style", selection: $newApiStyle) {
                            ForEach(ApiStyleModel.allCases, id: \.self) { style in
                                Text(style.rawValue).tag(style)
                            }
                        }
                        #if os(macOS)
                        .pickerStyle(.radioGroup)
                        #else
                        .pickerStyle(.wheel)
                        #endif
                        .padding()
                    }
            }

            TextField(String(localized: "providers.apiKeyOptionalPlaceholder"), text: $newApiKey).textFieldStyle(.roundedBorder)

            HStack {
                Button(String(localized: "button.cancel")) { isPresented = false }
                Spacer()
                Button(String(localized: "button.save")) {
                    let name = newName.isEmpty ? String(localized: "providers.customProviderDefault") : newName
                    let url = newBaseUrl.isEmpty ? String(localized: "providers.exampleUrl") : newBaseUrl
                    settingsVM.addCustomProvider(name: name, apiStyle: newApiStyle, baseUrl: url)
                    if !newApiKey.isEmpty {
                        // Provider ID will be generated - we save after creation
                    }
                    isPresented = false
                }
                .buttonStyle(.borderedProminent)
                .disabled(newName.isEmpty || newBaseUrl.isEmpty)
            }
        }
        .padding()
        .frame(width: 350)
    }
}

struct ModelListView: View {
    let models: [ModelDefModel]
    let onToggle: (String) -> Void

    @State private var filter = ""

    private var filtered: [ModelDefModel] {
        let result = models.filter { filter.isEmpty || $0.officialName.localizedCaseInsensitiveContains(filter) }
        return result.sorted { ($0.enabled ? 0 : 1) < ($1.enabled ? 0 : 1) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                TextField(String(localized: "providers.filterModelsPlaceholder"), text: $filter)
                    .textFieldStyle(.roundedBorder)

                Button(String(localized: "button.allOff")) {
                    filtered.filter { $0.enabled }.forEach { onToggle($0.id) }
                }
                .font(.body)

                Button(String(localized: "button.allOn")) {
                    filtered.filter { !$0.enabled }.forEach { onToggle($0.id) }
                }
                .font(.body)
            }

            Text(String(localized: "models.label")).font(.body).padding(.top, 4)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(filtered) { model in
                        HStack(spacing: 8) {
                            Toggle("", isOn: Binding(
                                get: { model.enabled },
                                set: { _ in onToggle(model.id) }
                            ))
                            .toggleStyle(.switch)
                            .labelsHidden()
                            .scaleEffect(0.7, anchor: .leading)
                            Text(model.officialName)
                                .font(.body)
                            Spacer()
                        }
                        .padding(.vertical, 6)
                        Divider()
                    }
                }
            }
            .frame(maxHeight: 400)
        }
        .padding(.vertical, 4)
    }
}
