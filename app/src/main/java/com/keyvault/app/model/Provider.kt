package com.keyvault.app.model

/**
 * AI provider identity.
 *
 * Provider is an enum, not user-editable free text, so we can map each one to a
 * dedicated test-ping endpoint and a stable icon. New providers are easy to add:
 * extend the enum, add the endpoint in [OkHttpKeyTester], and a localized label.
 */
enum class Provider(
    val id: String,
    val title: String,
    val shortName: String,
    val supportsTest: Boolean
) {
    OPENAI("openai", "OpenAI", "OP", supportsTest = true),
    ANTHROPIC("anthropic", "Anthropic", "AN", supportsTest = true),
    GOOGLE("google", "Google Gemini", "GO", supportsTest = true),
    OPENROUTER("openrouter", "OpenRouter", "OR", supportsTest = true),
    AZURE_OPENAI("azure", "Azure OpenAI", "AZ", supportsTest = false),
    HUGGING_FACE("huggingface", "Hugging Face", "HF", supportsTest = true),
    COHERE("cohere", "Cohere", "CO", supportsTest = true),
    MISTRAL("mistral", "Mistral", "MI", supportsTest = true),
    TOGETHER("together", "Together AI", "TO", supportsTest = true),
    XAI_GROK("xaigrok", "xAI / Grok", "GR", supportsTest = true),
    REPLICATE("replicate", "Replicate", "RP", supportsTest = true),
    // "Other" doubles as the custom bucket: when a user types their own provider
    // name, it is stored verbatim in KeyEntity.providerId and surfaced via
    // providerLabel(..., providerId) instead of the "Other" title.
    OTHER("other", "Other", "OT", supportsTest = false);

    companion object {
        fun fromId(id: String?): Provider = entries.firstOrNull { it.id == id } ?: OTHER
        fun all(): List<Provider> = entries.toList()
    }
}
