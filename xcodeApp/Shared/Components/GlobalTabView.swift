import SwiftUI

struct GlobalTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel

    var body: some View {
        VStack {
            TextEditor(text: Binding(
                get: { settingsVM.globalInstructions },
                set: { settingsVM.globalInstructions = $0; settingsVM.setGlobalInstructions($0) }
            ))
            .font(.body)
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
                RoundedRectangle(cornerRadius: 4)
                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
            )
        }
        .padding(.vertical, 8)
        .frame(maxHeight: .infinity)
    }
}
