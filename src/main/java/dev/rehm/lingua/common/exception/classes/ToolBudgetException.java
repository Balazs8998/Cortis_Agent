package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class ToolBudgetException extends BaseException {

    public ToolBudgetException(
            String functionName,
            String fieldName,
            Object value
    ) {
        super(
                functionName,
                fieldName,
                value,
                "Invalid tool call budget value",
                "INVALID_TOOL_BUDGET"
        );
    }
}