package dev.rehm.lingua.exception.classes;

import dev.rehm.lingua.exception.BaseException;

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
