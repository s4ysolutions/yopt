import SwiftUI
import UniformTypeIdentifiers

struct ExportTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SaveFileButton(label: "Export Settings") {
                settingsVM.export()
            }
            if let err = settingsVM.exportError {
                Text(err).foregroundColor(.red).font(.caption)
            }

            OpenFileButton(label: "Load & Replace") { content in
                settingsVM.importReplace(json: content)
            }
            if let err = settingsVM.importReplaceError {
                Text(err).foregroundColor(.red).font(.caption)
            }

            OpenFileButton(label: "Load & Append") { content in
                settingsVM.importAppend(json: content)
            }
            if let err = settingsVM.importAppendError {
                Text(err).foregroundColor(.red).font(.caption)
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
