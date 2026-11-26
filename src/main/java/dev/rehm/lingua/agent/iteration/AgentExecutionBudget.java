package dev.rehm.lingua.agent.iteration;

import dev.rehm.lingua.common.exception.classes.IterationValueException;
import dev.rehm.lingua.common.exception.classes.ToolBudgetException;

public class AgentExecutionBudget {


    int remainingIterations;
    int remainingToolCall;
    int iterations;

    public AgentExecutionBudget(int remainingIterations, int remainingToolCall) {
        this.remainingToolCall = remainingToolCall< 0 ? 4 : remainingToolCall;
        this.remainingIterations = remainingIterations<= 0 ? 6 : remainingIterations;
        this.iterations = 0;
    }

    public AgentExecutionBudget(int remainingToolCall) {
        this.remainingToolCall = remainingToolCall< 0 ? 4 : remainingToolCall;
        this.remainingIterations = remainingToolCall<= 0 ? 6 : remainingToolCall + 2;
        this.iterations = 0;
    }


    public void loopStep() {

        if (remainingIterations <= 0) {
            throw new IterationValueException(
                    "loopStep",
                    "remainingIterations",
                    remainingIterations
            );
        }

        this.remainingIterations--;
        this.iterations++;
    }

    public int reserveBudget(int requestedToolCalls) {

        if (requestedToolCalls < 0) {
            throw new ToolBudgetException(
                    "reserveBudget",
                    "requestedToolCalls",
                    requestedToolCalls
            );
        }

        int approved = Math.min(
                requestedToolCalls,
                remainingToolCall
        );

        remainingToolCall -= approved;

        return approved;
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
