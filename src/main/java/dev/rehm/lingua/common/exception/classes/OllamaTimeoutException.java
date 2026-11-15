package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class OllamaTimeoutException extends BaseException {

    public OllamaTimeoutException(
            String functionName,
            String fieldName,
            Object value
    ) {
        super(
                functionName,
                fieldName,
                value,
                "Ollama request timed out",
                "OLLAMA_TIMEOUT"
        );
    }
}
