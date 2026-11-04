package dev.rehm.lingua.exception;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public abstract class BaseException extends RuntimeException{
    private final String code;
    private final String functionName;
    private final String fieldName;
    private final Object value;
    private final Timestamp timestamp;
    private final String errorId;

    public BaseException(String functionName, String fieldName, Object value, String message, String code) {
        super(message);
        this.functionName = functionName;
        this.fieldName = fieldName;
        this.value = value;
        this.code = code;
        this.errorId = "ERR-"+ UUID.randomUUID();
        this.timestamp = Timestamp.from(Instant.now()) ;
    }

    public String getCode() {
        return code;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getValue() {
        return value;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getErrorId() {
        return errorId;
    }
}
