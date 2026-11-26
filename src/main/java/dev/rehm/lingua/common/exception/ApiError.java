package dev.rehm.lingua.common.exception;

import java.sql.Timestamp;

public record ApiError(
        String code,
        String errorId,
        Timestamp timestamp
) {
}



