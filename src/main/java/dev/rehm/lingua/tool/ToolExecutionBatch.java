package dev.rehm.lingua.tool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ToolExecutionBatch {

    private final long[] deadlinesNanos;
    private final AtomicReferenceArray<Instant> toolStartedAt;
    private final AtomicLongArray finishedAtNanos;
    private final AtomicReferenceArray<Thread> toolThreads;
    private final List<Future<ToolExecutionResult>> futures = new ArrayList<>();

    public ToolExecutionBatch(int approved) {
        this.deadlinesNanos = new long[approved];
        this.toolStartedAt = new AtomicReferenceArray<>(approved);
        this.finishedAtNanos = new AtomicLongArray(approved);
        this.toolThreads = new AtomicReferenceArray<>(approved);
    }



    public void addFuture(Future<ToolExecutionResult> future){
        futures.add(future);
    }


    public Future<ToolExecutionResult> getFuture(int index) {
        return this.futures.get(index) ;
    }

    public List<Future<ToolExecutionResult>> getFutures(){
        return List.copyOf(futures);
    }


    public long getDeadlinesNanos(int index) {
        return deadlinesNanos[index];
    }

    public void setDeadlinesNanos(int index, long deadline){
        deadlinesNanos[index] = deadline;
    }


    public Instant getToolStartedAt(int index) {
        return toolStartedAt.get(index);
    }

    public void setToolStartedAt(int index, Instant startedAt){
        toolStartedAt.set(index,startedAt);
    }



    public long getFinishedAtNanos(int index) {
        return finishedAtNanos.get(index);
    }

    public void setFinishedAtNanos(int index, long finishedAtNanos){
        this.finishedAtNanos.set(index,finishedAtNanos);
    }


    public Thread getToolThreads(int index) {
        return toolThreads.get(index);
    }

    public void setToolThread(int index, Thread thread){
        this.toolThreads.set(index, thread);
    }



}
