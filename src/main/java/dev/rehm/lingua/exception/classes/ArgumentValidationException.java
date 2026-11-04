package dev.rehm.lingua.exception.classes;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class IllegalToolArgumentException extends RuntimeException{

    private final String code;
    private final String functionName;
    private final String fieldName;
    private final Object value;
    private final Timestamp timestamp;
    private final String errorId;

    public IllegalToolArgumentException(String functionName, String fieldName, Object value, String message, String code, String errorId) {
        super(message);
        this.functionName = functionName;
        this.fieldName = fieldName;
        this.value = value;
        this.code = code;
        this.errorId = "ERR-"+UUID.randomUUID();
        this.timestamp = Timestamp.from(Instant.now()) ;
    }


}
