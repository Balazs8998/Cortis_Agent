package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class ConversationNotFoundException extends BaseException {

    public ConversationNotFoundException(
            String functionName,
            String fieldName,
            Object value
    ) {
        super(
                functionName,
                fieldName,
                value,
                "Conversation not found: " + value,
                "CONVERSATION_NOT_FOUND"
        );
    }
}