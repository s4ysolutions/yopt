import SwiftUI

struct GlobalTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Global Instructions")
                .font(.caption)
                .fontWeight(.semibold)
                .textCase(.uppercase)
                .foregroundColor(.secondary)

            TextEditor(text: Binding(
                get: { settingsVM.globalInstructions },
                set: { settingsVM.globalInstructions = $0; settingsVM.setGlobalInstructions($0) }
            ))
            .font(.body)
            .scrollContentBackground(.hidden)
            .frame(maxHeight: .infinity)
            .overlay(
                Group {
                    if settingsVM.globalInstructions.isEmpty {
                        Text("Enter global instructions for all chats...")
                            .foregroundColor(.secondary)
                            .padding(.leading, 4)
                            .padding(.top, 8)
                            .allowsHitTesting(false)
                    }
                },
                alignment: .topLeading
            )
            .overlay(
                RoundedRectangle(cornerRadius: 6)
                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
            )
        }
        .padding(DesignTokens.sectionPadding)
        .frame(maxHeight: .infinity)
    }
}
