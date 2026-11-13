package dev.rehm.lingua.inspection.dto;

import java.time.Instant;
import java.util.Map;

public record ToolExecutionResult(

        String toolName,
        Map<String,Object> arguments,
        Object result,
        ToolExecutionStatus status,
        Instant executedAt,
        double durationMs,
        Long startThreadId,
        boolean startIsVirtual,
        Integer toolCallIndex


) {
}
