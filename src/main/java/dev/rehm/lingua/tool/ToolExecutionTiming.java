
package dev.rehm.lingua.tool;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class ToolExecutionTiming {

    private final double durationMs;
    private final Instant executedAt;

    public ToolExecutionTiming(
            ToolExecutionBatch toolExecutionBatch,
            int index
    ) {
        long deadline = toolExecutionBatch.getDeadlinesNanos(index);

        long startNanos =
                deadline - TimeUnit.SECONDS.toNanos(5);

        long finishedNanos =
                toolExecutionBatch.getFinishedAtNanos(index);

        long endNanos = finishedNanos != 0L
                ? finishedNanos
                : Math.min(System.nanoTime(), deadline);

        this.durationMs =
                Math.max(0L, endNanos - startNanos)
                        / 1_000_000.0;

        Instant startedAt =
                toolExecutionBatch.getToolStartedAt(index);

        this.executedAt = startedAt != null
                ? startedAt
                : Instant.now();
    }

    public double getDurationMs() {
        return durationMs;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
