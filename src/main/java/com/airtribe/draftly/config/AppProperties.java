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

    /** HMAC secret (Base64) used to sign/verify JWTs (login + OAuth state). */
    private String jwtSecret = "NjxQyuBSl/yBIsYuWTciUZTEta6TJ2H15rijFU4NdVg=";

    /** How long an issued login JWT remains valid. */
    private int jwtExpirationMinutes = 1440;

    /** How many past sent emails to retrieve as style context for RAG. */
    private int ragTopK = 3;

    /** Which embedding implementation to use: "hashing" or "openai". */
    private String embeddingProvider = "hashing";

    /** Where style-sample vectors are searched: "memory" (Java cosine) or "pgvector". */
    private String vectorStore = "memory";

    /** Maximum automatic retry attempts for a failed send. */
    private int maxSendRetries = 3;

    /** Comma-separated origins allowed to call the API from a browser (the frontend's dev/prod URLs). */
    private String corsAllowedOrigins = "http://localhost:5173";

    private final Anthropic anthropic = new Anthropic();
    private final OpenAi openai = new OpenAi();
    private final Gmail gmail = new Gmail();

    public static class OpenAi {
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com/v1/chat/completions";
        private String embeddingModel = "text-embedding-3-small";
        private String embeddingsBaseUrl = "https://api.openai.com/v1/embeddings";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public String getEmbeddingsBaseUrl() { return embeddingsBaseUrl; }
        public void setEmbeddingsBaseUrl(String embeddingsBaseUrl) { this.embeddingsBaseUrl = embeddingsBaseUrl; }
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
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public int getJwtExpirationMinutes() { return jwtExpirationMinutes; }
    public void setJwtExpirationMinutes(int jwtExpirationMinutes) { this.jwtExpirationMinutes = jwtExpirationMinutes; }
    public int getRagTopK() { return ragTopK; }
    public void setRagTopK(int ragTopK) { this.ragTopK = ragTopK; }
    public String getEmbeddingProvider() { return embeddingProvider; }
    public void setEmbeddingProvider(String embeddingProvider) { this.embeddingProvider = embeddingProvider; }
    public String getVectorStore() { return vectorStore; }
    public void setVectorStore(String vectorStore) { this.vectorStore = vectorStore; }
    public int getMaxSendRetries() { return maxSendRetries; }
    public void setMaxSendRetries(int maxSendRetries) { this.maxSendRetries = maxSendRetries; }
    public String getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(String corsAllowedOrigins) { this.corsAllowedOrigins = corsAllowedOrigins; }
    public Anthropic getAnthropic() { return anthropic; }
    public OpenAi getOpenai() { return openai; }
    public Gmail getGmail() { return gmail; }
}
