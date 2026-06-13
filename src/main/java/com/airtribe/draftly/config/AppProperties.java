package com.airtribe.draftly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration bound from the {@code draftly.*} keys in
 * application.yml. Keeping config in one place makes the app easy to tune
 * without hunting through @Value annotations.
 */
@ConfigurationProperties(prefix = "draftly")
public class AppProperties {

    /** Which Gmail implementation to use: "mock" or "real". */
    private String gmailMode = "mock";

    /** Which LLM provider to use: "mock" or "anthropic". */
    private String llmProvider = "mock";

    /** AES secret (Base64, 16/24/32 bytes) used to encrypt OAuth tokens at rest. */
    private String encryptionKey = "ZHJhZnRseS1kZW1vLWtleS0xMjM0NTY3OA==";

    /** How many past sent emails to retrieve as style context for RAG. */
    private int ragTopK = 3;

    /** Maximum automatic retry attempts for a failed send. */
    private int maxSendRetries = 3;

    private final Anthropic anthropic = new Anthropic();
    private final OpenAi openai = new OpenAi();
    private final Gmail gmail = new Gmail();

    public static class OpenAi {
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com/v1/chat/completions";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Anthropic {
        private String apiKey = "";
        private String model = "claude-sonnet-4-6";
        private String baseUrl = "https://api.anthropic.com/v1/messages";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Gmail {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "http://localhost:8080/api/auth/gmail/callback";

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public String getGmailMode() { return gmailMode; }
    public void setGmailMode(String gmailMode) { this.gmailMode = gmailMode; }
    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }
    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    public int getRagTopK() { return ragTopK; }
    public void setRagTopK(int ragTopK) { this.ragTopK = ragTopK; }
    public int getMaxSendRetries() { return maxSendRetries; }
    public void setMaxSendRetries(int maxSendRetries) { this.maxSendRetries = maxSendRetries; }
    public Anthropic getAnthropic() { return anthropic; }
    public OpenAi getOpenai() { return openai; }
    public Gmail getGmail() { return gmail; }
}
