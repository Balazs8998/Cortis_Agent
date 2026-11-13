package dev.rehm.lingua.execution;

import dev.rehm.lingua.dto.response.OllamaChatResponse;

public class AgentExecutionBudget {

    int remainingToolCall;
    int remainingIterations;
    int iterations;

    public AgentExecutionBudget(int remainingToolCall, int remainingIterations) {
        this.remainingToolCall = remainingToolCall;
        this.remainingIterations = remainingIterations;
        this.iterations = 0;
    }

    public AgentExecutionBudget(int remainingToolCall) {
        this.remainingToolCall = remainingToolCall;
        this.remainingIterations = remainingToolCall + 2;
        this.iterations = 0;
    }

    public AgentExecutionBudget() {
    }

    public void loopStep(){
        decrementRemainingIterations();
        incrementIterations();
    }

    public int reserveBudget(int requestedToolCalls) {
        if (requestedToolCalls < 0) {
            throw new IllegalArgumentException(
                    "Requested tool calls cannot be negative"
            );
        }

        int approved = Math.min(
                requestedToolCalls,
                remainingToolCall
        );

        remainingToolCall -= approved;

        return approved;
    }

    public void decrementRemainingTools(){
        if (remainingToolCall> 0)
        this.remainingToolCall--;
    }

    public void decrementRemainingIterations(){
        if (remainingIterations > 0)
            this.remainingIterations--;
    }

    public void incrementIterations(){
        this.iterations++;
    }


    public int getRemainingToolCall() {
        return remainingToolCall;
    }

    public int getRemainingIterations() {
        return remainingIterations;
    }

    public int getIterations() {
        return iterations;
    }

    public void printLoopLimits() {
        System.out.println("Loop iteration count: " + this.iterations);
        System.out.println("Remaining loop iterations: " + this.remainingIterations);
        System.out.println("Remaining tool calls: " + this.remainingToolCall);
    }
}
