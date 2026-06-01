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
            }

            Button(action: { showAddCustom = true }) {
                Label("Add Custom Provider", systemImage: "plus.circle")
            }
            .popover(isPresented: $showAddCustom) {
                AddCustomProviderView(settingsVM: settingsVM, isPresented: $showAddCustom)
            }
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
                    Text(hasKey ? "API Key set" : "Not configured")
                        .font(.caption)
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
                    .help("Refresh Models")
                }

                Button(action: onToggle) {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                }
                .buttonStyle(.plain)
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
        .cornerRadius(8)
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
                Button("Change") { editing = true }
                Button("Clear", role: .destructive, action: onClear)
            }
        } else {
            VStack(alignment: .leading, spacing: 8) {
                TextField(hasKey ? "New API Key" : provider.name + " API Key", text: $key)
                    .textFieldStyle(.roundedBorder)

                if !hasKey {
                    let url = providerUrl(for: provider.id)
                    if let url = url {
                        Link("Get API Key \u{2197}", destination: url)
                            .font(.caption)
                    }
                }

                HStack(spacing: 8) {
                    Button("Save") {
                        onSave(key)
                        key = ""
                        editing = false
                    }
                    .buttonStyle(.borderedProminent)

                    if hasKey {
                        Button("Cancel") {
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
            TextField("Provider Name", text: $editName)
                .textFieldStyle(.roundedBorder)
            TextField("Base URL", text: $editBaseUrl)
                .textFieldStyle(.roundedBorder)

            HStack {
                Text("API Style: \(editApiStyle.rawValue)")
                Spacer()
                Button("Change") { styleExpanded.toggle() }
                    .popover(isPresented: $styleExpanded) {
                        Picker("API Style", selection: $editApiStyle) {
                            ForEach(ApiStyleModel.allCases, id: \.self) { style in
                                Text(style.rawValue).tag(style)
                            }
                        }
                        .pickerStyle(.radioGroup)
                        .padding()
                    }
            }

            TextField("API Key", text: $editApiKey)
                .textFieldStyle(.roundedBorder)

            HStack {
                Button("Save") {
                    onUpdate(editName, editBaseUrl, editApiStyle)
                    if !editApiKey.isEmpty {
                        onSave(editApiKey)
                    }
                }
                .buttonStyle(.borderedProminent)

                Button("Delete", role: .destructive, action: onDelete)
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
            Text("Add Custom Provider").font(.headline)
            TextField("Provider Name", text: $newName).textFieldStyle(.roundedBorder)
            TextField("Base URL", text: $newBaseUrl).textFieldStyle(.roundedBorder)

            HStack {
                Text("API Style: \(newApiStyle.rawValue)")
                Spacer()
                Button("Change") { styleExpanded.toggle() }
                    .popover(isPresented: $styleExpanded) {
                        Picker("API Style", selection: $newApiStyle) {
                            ForEach(ApiStyleModel.allCases, id: \.self) { style in
                                Text(style.rawValue).tag(style)
                            }
                        }
                        .pickerStyle(.radioGroup)
                        .padding()
                    }
            }

            TextField("API Key (optional)", text: $newApiKey).textFieldStyle(.roundedBorder)

            HStack {
                Button("Cancel") { isPresented = false }
                Spacer()
                Button("Save") {
                    let name = newName.isEmpty ? "Custom Provider" : newName
                    let url = newBaseUrl.isEmpty ? "https://api.example.com" : newBaseUrl
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
        return result.sorted { $0.enabled && !$1.enabled }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                TextField("Filter models...", text: $filter)
                    .textFieldStyle(.roundedBorder)

                Button("All Off") {
                    filtered.forEach { onToggle($0.id) }
                }
                .font(.caption2)

                Button("All On") {
                    filtered.forEach { onToggle($0.id) }
                }
                .font(.caption2)
            }

            Text("Models").font(.caption).padding(.top, 4)

            ForEach(filtered) { model in
                Toggle(isOn: Binding(
                    get: { model.enabled },
                    set: { _ in onToggle(model.id) }
                )) {
                    Text(model.officialName)
                        .font(.body)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
