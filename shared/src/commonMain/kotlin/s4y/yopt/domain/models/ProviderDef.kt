package s4y.yopt.domain.models

import kotlinx.serialization.Serializable

enum class ApiStyle { OPENAI, ANTHROPIC, GEMINI }

@Serializable
data class ProviderDef(
    val id: String,
    val name: String,
    val apiStyle: ApiStyle,
    val authType: AuthType,
    val baseUrl: String,
    val predefined: Boolean = true
) {
    companion object {
        val predefined = listOf(
            ProviderDef("openrouter", "OpenRouter", ApiStyle.OPENAI, AuthType.ApiKey("OpenRouter API Key"), "https://openrouter.ai/api/v1"),
            ProviderDef("openai", "OpenAI", ApiStyle.OPENAI, AuthType.ApiKey("OpenAI API Key"), "https://api.openai.com/v1"),
            ProviderDef("google", "Google Gemini", ApiStyle.GEMINI, AuthType.ApiKey("Gemini API Key"), "https://generativelanguage.googleapis.com/v1beta"),
            ProviderDef("anthropic", "Anthropic", ApiStyle.ANTHROPIC, AuthType.ApiKey("Anthropic API Key"), "https://api.anthropic.com/v1"),
            ProviderDef("deepseek", "DeepSeek", ApiStyle.OPENAI, AuthType.ApiKey("DeepSeek API Key"), "https://api.deepseek.com/v1"),
            ProviderDef("qwen", "Qwen", ApiStyle.OPENAI, AuthType.ApiKey("Qwen API Key"), "https://dashscope-intl.aliyuncs.com/compatible-mode/v1"),
            ProviderDef("xai", "xAI Grok", ApiStyle.OPENAI, AuthType.ApiKey("xAI API Key"), "https://api.x.ai/v1"),
        )
    }
}
