import SwiftUI
import UniformTypeIdentifiers

struct ExportTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.spacing12) {
            SaveFileButton(label: String(localized: "export.label")) {
                settingsVM.export()
            }
            if let err = settingsVM.exportError {
                Text(err).foregroundColor(.red).font(.body)
            }

            OpenFileButton(label: String(localized: "import.replaceLabel")) { content in
                settingsVM.importReplace(json: content)
            }
            if let err = settingsVM.importReplaceError {
                Text(err).foregroundColor(.red).font(.body)
            }

            OpenFileButton(label: String(localized: "import.appendLabel")) { content in
                settingsVM.importAppend(json: content)
            }
            if let err = settingsVM.importAppendError {
                Text(err).foregroundColor(.red).font(.body)
            }
        }
        .padding(.top, DesignTokens.sectionPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .alert(settingsVM.dialogTitle ?? "", isPresented: Binding(
            get: { settingsVM.dialogText != nil },
            set: { if !$0 { settingsVM.dialogText = nil } }
        )) {
            Button(String(localized: "dialog.ok")) { settingsVM.dialogText = nil }
        } message: {
            Text(settingsVM.dialogText ?? "")
        }
    }
}

struct SaveFileButton: View {
    let label: String
    let onSave: () -> Void

    var body: some View {
        Button(action: onSave) {
            Label(label, systemImage: "square.and.arrow.up")
        }
    }
}

struct OpenFileButton: View {
    let label: String
    let onLoad: (String) -> Void

    @State private var isPresented = false

    var body: some View {
        Button(action: { isPresented = true }) {
            Label(label, systemImage: "square.and.arrow.down")
        }
        .fileImporter(isPresented: $isPresented, allowedContentTypes: [.json]) { result in
            switch result {
            case .success(let url):
                if let data = try? Data(contentsOf: url), let content = String(data: data, encoding: .utf8) {
                    onLoad(content)
                }
            case .failure:
                break
            }
        }
    }
}
