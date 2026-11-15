package dev.rehm.lingua.llm;

import dev.rehm.lingua.common.exception.classes.UnsupportedLlmModelException;
import org.springframework.stereotype.Service;


@Service
public class LlmProfileService {

    public LlmProfiles createLlmProfiles(String model, boolean think) {
        return switch (model) {
            case "qwen3.5:4b" -> new LlmProfiles(
                    think
                            ? LlmProfileFactory.qwen35Thinking()
                            : LlmProfileFactory.qwen35Normal(),
                    LlmProfileFactory.qwen35Final()
            );

            case "qwen3:8b" -> new LlmProfiles(
                    think
                            ? LlmProfileFactory.qwen3Thinking()
                            : LlmProfileFactory.qwen3Normal(),
                    LlmProfileFactory.qwen3Final()
            );

            case "gpt-oss:20b" -> new LlmProfiles(
                    think
                            ? LlmProfileFactory.gptOssThinking()
                            : LlmProfileFactory.gptOssNormal(),
                    LlmProfileFactory.gptOssFinal()
            );

            default -> throw new UnsupportedLlmModelException(
                    "createLlmProfiles",
                    "model",
                    model
            );
        };
    }
}