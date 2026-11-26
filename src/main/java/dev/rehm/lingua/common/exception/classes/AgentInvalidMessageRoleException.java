package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class AgentInvalidMessageRoleException extends BaseException {

    public AgentInvalidMessageRoleException(
            String functionName,
            String fieldName,
            String role
    ) {
        super(
                functionName,
                fieldName,
                role,
                "Agent execution failed: Expected assistant role, but received: " + role,
                "AGENT_INVALID_MESSAGE_ROLE"
        );
    }
}