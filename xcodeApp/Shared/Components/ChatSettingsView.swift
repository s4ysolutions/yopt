import SwiftUI

struct ChatSettingsView: View {
    @Binding var isPresented: Bool
    let chat: ChatModel
    let onSave: (String, String, [String]) -> Void

    @State private var instructions: String = ""
    @State private var labels: [String] = []
    @State private var showAddTag = false
    @FocusState private var focusedField: FocusField?

    enum FocusField {
        case instructions
        case tags
    }

    private func save() {
        onSave(chat.title, instructions, labels)
        isPresented = false
    }

    var body: some View {
        VStack(spacing: DesignTokens.spacing16) {
#if os(macOS)
            HStack {
                Text(String(localized: "chatSettings.title"))
                    .font(.title2)
                    .fontWeight(.semibold)
                Spacer()
                Button(action: { isPresented = false }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
                .help(String(localized: "help.close"))
            }
            .padding(.bottom, DesignTokens.padding8)
            Divider()
#else
            Text(String(localized: "chatSettings.title"))
                .font(.title3.weight(.semibold))
#endif

            VStack(alignment: .leading, spacing: DesignTokens.spacing6) {
                Text(String(localized: "chatSettings.instructions"))
                    .font(.caption)
                    .fontWeight(.semibold)
                    .textCase(.uppercase)
                    .foregroundColor(.secondary)
                TextEditor(text: $instructions)
                    .font(.body)
                    .scrollContentBackground(.hidden)
                    .frame(height: DesignTokens.chatSettingsInstructionsHeight)
                    .overlay(
                        RoundedRectangle(cornerRadius: DesignTokens.cornerRadius6)
                            .stroke(Color.secondary.opacity(DesignTokens.opacity30), lineWidth: 1)
                    )
                    .focused($focusedField, equals: .instructions)
            }

            VStack(alignment: .leading, spacing: DesignTokens.spacing6) {
                Text(String(localized: "chatSettings.tags"))
                    .font(.caption)
                    .fontWeight(.semibold)
                    .textCase(.uppercase)
                    .foregroundColor(.secondary)
                TagChipsView(tags: $labels, onAddTag: { showAddTag = true })
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .focused($focusedField, equals: .tags)
            }

            Spacer()
                .frame(height: DesignTokens.padding8)

            HStack(spacing: DesignTokens.spacing12) {
                Button(String(localized: "button.cancel")) { isPresented = false }
                    .keyboardShortcut(.cancelAction)
                Spacer()
                Button(String(localized: "button.save")) { save() }
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut("s", modifiers: [.command])
            }
        }
        .padding(DesignTokens.padding16)
#if os(macOS)
        .frame(minWidth: DesignTokens.chatSettingsMinWidth, idealWidth: DesignTokens.chatSettingsIdealWidth)
        .fixedSize(horizontal: false, vertical: true)
        .presentationDragIndicator(.hidden)
#else
        .presentationDetents([.medium])
#endif
        .sheet(isPresented: $showAddTag) {
            AddTagDialog(isPresented: $showAddTag, tags: $labels)
        }
        .onAppear {
            instructions = chat.instructions
            labels = chat.labels
            #if os(macOS)
            focusedField = .instructions
            #endif
        }
    }
}
