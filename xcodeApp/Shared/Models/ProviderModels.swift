import Foundation
import ComposeApp

/// Mirrors ProviderDef (from KMP YoPtShared)
struct ProviderModel: Identifiable, Equatable {
    let id: String
    let name: String
    let apiStyle: ApiStyleModel
    let baseUrl: String
    let predefined: Bool

    static func fromKotlin(_ provider: ProviderDef) -> ProviderModel {
        ProviderModel(
            id: provider.id,
            name: provider.name,
            apiStyle: ApiStyleModel.fromKotlin(provider.apiStyle),
            baseUrl: provider.baseUrl,
            predefined: provider.predefined
        )
    }

    func toKotlinProvider() -> ProviderDef {
        ProviderDef(
            id: id,
            name: name,
            apiStyle: apiStyle.toKotlin(),
            authType: AuthType.ApiKey(keyName: "\(name) API Key"),
            baseUrl: baseUrl,
            predefined: predefined
        )
    }
}

/// Mirrors ApiStyle (from KMP YoPtShared)
enum ApiStyleModel: String, CaseIterable {
    case openai = "OPENAI"
    case anthropic = "ANTHROPIC"
    case gemini = "GEMINI"

    static func fromKotlin(_ style: ApiStyle) -> ApiStyleModel {
        switch style {
        case .openai: return .openai
        case .anthropic: return .anthropic
        case .gemini: return .gemini
        }
    }

    func toKotlin() -> ApiStyle {
        switch self {
        case .openai: return .openai
        case .anthropic: return .anthropic
        case .gemini: return .gemini
        }
    }
}

/// Mirrors ModelDef (from KMP YoPtShared)
struct ModelDefModel: Identifiable, Equatable {
    let id: String
    let providerId: String
    let officialName: String
    let enabled: Bool

    var displayName: String { officialName }

    static func fromKotlin(_ model: ModelDef) -> ModelDefModel {
        ModelDefModel(
            id: model.id,
            providerId: model.providerId,
            officialName: model.officialName,
            enabled: model.enabled
        )
    }
}

/// Mirrors AuthCredentials (from KMP YoPtShared)
struct AuthCredentialsModel: Equatable {
    let providerId: String
    let apiKey: String?

    static func fromKotlin(_ cred: AuthCredentials) -> AuthCredentialsModel {
        AuthCredentialsModel(providerId: cred.providerId, apiKey: cred.apiKey)
    }
}
