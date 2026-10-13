package com.example.model

enum class AiProvider(val title: String, val badge: String) {
    GEMINI("Google Gemini (内置云端托管)", "内置云端"),
    DEEPSEEK("DeepSeek (外部 API 导入)", "第三方直连"),
    OPENAI_COMPATIBLE("OpenAI 兼容接口 (中继/私有部署)", "自定义中继"),
    OFFLINE_ACADEMIC("内置高精度学术规则引擎 (离线/免Key)", "零耗额·永不超限")
}

data class AiModelConfig(
    val provider: AiProvider = AiProvider.GEMINI,
    val geminiModelName: String = "gemini-3.6-flash",
    val customGeminiApiKey: String = "",
    val customBaseUrl: String = "https://api.deepseek.com/v1",
    val customApiKey: String = "",
    val customModelName: String = "deepseek-chat"
) {
    val activeDisplayName: String
        get() = when (provider) {
            AiProvider.GEMINI -> "Gemini: $geminiModelName"
            AiProvider.DEEPSEEK -> "DeepSeek: $customModelName"
            AiProvider.OPENAI_COMPATIBLE -> "OpenAI: $customModelName"
            AiProvider.OFFLINE_ACADEMIC -> "本地高精度学术引擎"
        }

    val isReady: Boolean
        get() = when (provider) {
            AiProvider.GEMINI -> true
            AiProvider.OFFLINE_ACADEMIC -> true
            AiProvider.DEEPSEEK, AiProvider.OPENAI_COMPATIBLE -> customApiKey.isNotBlank()
        }
}
