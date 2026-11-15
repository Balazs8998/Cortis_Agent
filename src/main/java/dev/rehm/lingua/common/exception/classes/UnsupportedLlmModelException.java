package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class UnsupportedLlmModelException extends BaseException {

    public UnsupportedLlmModelException(
            String functionName,
            String fieldName,
            Object value
    ) {
        super(
                functionName,
                fieldName,
                value,
                "Unsupported LLM model: " + value,
                "UNSUPPORTED_LLM_MODEL"
        );
    }
}