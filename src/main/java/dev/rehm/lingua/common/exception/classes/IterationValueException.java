package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class IterationValueException extends BaseException {

    public IterationValueException(
            String functionName,
            String fieldName,
            Object value
    ) {
        super(
                functionName,
                fieldName,
                value,
                "Invalid iteration value",
                "INVALID_ITERATION_VALUE"
        );
    }
}