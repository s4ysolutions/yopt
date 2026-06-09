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
        VStack(spacing: 8) {
            TextEditor(text: $prompt)
                .font(.body)
                .scrollContentBackground(.hidden)
                .frame(minHeight: 60, maxHeight: .infinity)
                .overlay(
                    Group {
                        if prompt.isEmpty {
                            Text("Enter prompt...")
                                .foregroundColor(.secondary)
                                .padding(.leading, 4)
                                .padding(.top, 8)
                                .allowsHitTesting(false)
                        }
                    },
                    alignment: .topLeading
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                )

            VStack(spacing: 8) {
                HStack {
                    Button(action: {
                        if modelsEmpty { onOpenSettings() }
                        else { showModelPicker.toggle() }
                    }) {
                        Text(selectedModelName)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                    .popover(isPresented: $showModelPicker) {
                        modelPickerContent
                            .presentationDetents([.height(min(CGFloat(models.count) * 44 + 32, 380))])
                    }

                    if loading {
                        Button(action: onCancel) {
                            ProgressView()
                                .scaleEffect(0.8)
                                .frame(width: 28, height: 28)
                        }
                        .buttonStyle(.plain)
                    } else {
                        Button("Send", action: onSend)
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
        }
        .background(GeometryReader { proxy in
            Color.clear.preference(key: ChatTopPanelMinHeight.self, value: proxy.size.height)
        })
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
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(8)
        }
        #if os(macOS)
        .frame(width: 280, height: min(CGFloat(models.count) * 36 + 8, 300))
        #endif
    }
}
