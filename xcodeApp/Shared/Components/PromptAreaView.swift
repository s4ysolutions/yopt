import SwiftUI

struct PromptAreaView: View {
    @Binding var prompt: String
    @Binding var loading: Bool
    let selectedModelName: String
    let selectedModelId: String?
    let models: [ModelDefModel]
    let modelsEmpty: Bool
    let error: String?
    let onSend: () -> Void
    let onCancel: () -> Void
    let onSelectModel: (String) -> Void
    let onOpenSettings: () -> Void

    @State private var showModelPicker = false

    var body: some View {
        VStack(spacing: DesignTokens.spacing8) {
            TextEditor(text: $prompt)
                .font(.body)
                .scrollContentBackground(.hidden)
                .frame(minHeight: DesignTokens.textEditorMinHeight, maxHeight: .infinity)
                .overlay(
                    Group {
                        if prompt.isEmpty {
                            Text(String(localized: "prompt.placeholder"))
                                .foregroundColor(.secondary)
                                .padding(.leading, DesignTokens.padding4)
                                .padding(.top, DesignTokens.padding8)
                                .allowsHitTesting(false)
                        }
                    },
                    alignment: .topLeading
                )
                .overlay(
                    RoundedRectangle(cornerRadius: DesignTokens.cornerRadius4)
                        .stroke(Color.secondary.opacity(DesignTokens.opacity30), lineWidth: 1)
                )
                .background(Color.clear.preference(key: ChatTopPanelMinHeight.self, value: DesignTokens.textEditorMinHeight))

            VStack(spacing: DesignTokens.spacing8) {
                HStack {
                    Button(action: {
                        if modelsEmpty { onOpenSettings() }
                        else { showModelPicker.toggle() }
                    }) {
                        Text(selectedModelName)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, DesignTokens.padding12)
                            .padding(.vertical, DesignTokens.padding6)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .overlay(
                        RoundedRectangle(cornerRadius: DesignTokens.cornerRadius6)
                            .stroke(Color.secondary.opacity(DesignTokens.opacity30), lineWidth: 1)
                    )
                    .popover(isPresented: $showModelPicker) {
                        modelPickerContent
                            .presentationDetents([.medium, .large])
                    }

                    if loading {
                        Button(action: onCancel) {
                            ProgressView()
                                .scaleEffect(0.8)
                                .frame(width: 28, height: 28)
                        }
                        .buttonStyle(.plain)
                    } else {
                        Button(String(localized: "prompt.send"), action: onSend)
                            .buttonStyle(.borderedProminent)
                            .keyboardShortcut(.return, modifiers: [.command])
                    }
                }

                if let err = error {
                    Text(err)
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .background(GeometryReader { proxy in
                Color.clear.preference(key: ChatTopPanelMinHeight.self, value: proxy.size.height + DesignTokens.padding8)
            })
        }
    }

    private var modelPickerContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading) {
                ForEach(models) { model in
                    Button(action: {
                        onSelectModel(model.id)
                        showModelPicker = false
                    }) {
                        HStack {
                            Text(model.displayName)
                            Spacer()
                            if model.id == selectedModelId {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                        .padding(.horizontal, DesignTokens.padding16)
                        .padding(.vertical, DesignTokens.padding10)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(DesignTokens.padding8)
        }
        #if os(macOS)
        .frame(width: DesignTokens.chatListMinWidth, height: min(CGFloat(models.count) * 36 + DesignTokens.padding8, DesignTokens.modelPickerMinHeight))
        #endif
    }
}
