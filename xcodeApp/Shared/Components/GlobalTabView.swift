import SwiftUI

struct GlobalTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.spacing8) {
            Text(String(localized: "global.instructions"))
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
                        Text(String(localized: "global.instructions.placeholder"))
                            .foregroundColor(.secondary)
                            .padding(.leading, DesignTokens.padding4)
                            .padding(.top, DesignTokens.padding8)
                            .allowsHitTesting(false)
                    }
                },
                alignment: .topLeading
            )
            .overlay(
                RoundedRectangle(cornerRadius: DesignTokens.cornerRadius6)
                    .stroke(Color.secondary.opacity(DesignTokens.opacity30), lineWidth: 1)
            )
        }
        .padding(DesignTokens.sectionPadding)
        .frame(maxHeight: .infinity)
    }
}
