import SwiftUI

struct ModelSelectorView: View {
    let models: [ModelDefModel]
    let providers: [ProviderModel]
    @Binding var selectedModelId: String?
    let modelsEmpty: Bool
    let onOpenSettings: () -> Void

    @State private var expanded = false

    var selectedLabel: String {
        guard let sel = models.first(where: { $0.id == selectedModelId }) else {
            return "Select Model"
        }
        let prov = providers.first { $0.id == sel.providerId }
        if let pn = prov?.name {
            return "\(pn): \(sel.officialName)"
        }
        return sel.officialName
    }

    var body: some View {
        Button(action: {
            if modelsEmpty { onOpenSettings() }
            else { expanded.toggle() }
        }) {
            Text(selectedLabel)
                .lineLimit(1)
        }
        .popover(isPresented: $expanded) {
            List(models) { model in
                let provName = providers.first { $0.id == model.providerId }?.name
                Button(action: {
                    selectedModelId = model.id
                    expanded = false
                }) {
                    HStack {
                        Text(provName != nil ? "\(provName!): \(model.officialName)" : model.officialName)
                        Spacer()
                        if model.id == selectedModelId {
                            Image(systemName: "checkmark")
                        }
                    }
                }
                .buttonStyle(.plain)
            }
            .frame(width: 300, height: 400)
        }
    }
}
