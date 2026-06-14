import SwiftUI
import UniformTypeIdentifiers

struct JSONFile: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }
    var text: String
    init(_ text: String) { self.text = text }
    init(configuration: ReadConfiguration) throws {
        text = String(data: configuration.file.regularFileContents ?? Data(), encoding: .utf8) ?? ""
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}

struct ExportTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel

    @State private var isExporting = false

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.spacing12) {
            Button(action: { settingsVM.export() }) {
                Label(String(localized: "export.label"), systemImage: "square.and.arrow.up")
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
        .onChange(of: settingsVM.exportContent, perform: { content in
            if content != nil { isExporting = true }
        })
        .fileExporter(
            isPresented: $isExporting,
            document: settingsVM.exportContent.map { JSONFile($0) },
            contentType: .json,
            defaultFilename: "yopt-export"
        ) { _ in
            settingsVM.exportContent = nil
        }
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
                let accessing = url.startAccessingSecurityScopedResource()
                defer { if accessing { url.stopAccessingSecurityScopedResource() } }
                if let data = try? Data(contentsOf: url), let content = String(data: data, encoding: .utf8) {
                    onLoad(content)
                }
            case .failure:
                break
            }
        }
    }
}
