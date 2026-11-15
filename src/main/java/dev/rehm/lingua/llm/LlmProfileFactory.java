package dev.rehm.lingua.llm;

public final class LlmProfileFactory {

    private static final int NUM_CTX = 8192;

    private LlmProfileFactory() {
    }

    public static LlmProfile qwen35Thinking() {
        return create(
                "qwen3.5:4b", true,
                4096, 1.0, 0.95, 20
        );
    }

    public static LlmProfile qwen35Normal() {
        return create(
                "qwen3.5:4b", false,
                2048, 0.7, 0.8, 20
        );
    }

    public static LlmProfile qwen35Final() {
        return create(
                "qwen3.5:4b", false,
                1024, 0.7, 0.8, 20
        );
    }

    public static LlmProfile qwen3Thinking() {
        return create(
                "qwen3:8b", true,
                4096, 0.6, 0.95, 20
        );
    }

    public static LlmProfile qwen3Normal() {
        return create(
                "qwen3:8b", false,
                2048, 0.7, 0.8, 20
        );
    }

    public static LlmProfile qwen3Final() {
        return create(
                "qwen3:8b", false,
                1024, 0.7, 0.8, 20
        );
    }

    public static LlmProfile gptOssThinking() {
        return create(
                "gpt-oss:20b", "high",
                4096, 1.0, 1.0, 0
        );
    }

    public static LlmProfile gptOssNormal() {
        return create(
                "gpt-oss:20b", "low",
                2048, 1.0, 1.0, 0
        );
    }

    public static LlmProfile gptOssFinal() {
        return create(
                "gpt-oss:20b", "low",
                2048, 1.0, 1.0, 0
        );
    }

    private static LlmProfile create(
            String model,
            Object think,
            int numPredict,
            double temperature,
            double topP,
            int topK
    ) {
        return new LlmProfile(
                model,
                think,
                NUM_CTX,
                numPredict,
                temperature,
                topP,
                0.0,  // minP
                topK,
                1.0   // repeatPenalty
        );
    }
}