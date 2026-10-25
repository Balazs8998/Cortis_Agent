package dev.rehm.lingua.execution;

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

    public void decrementRemainingTools(){
        this.remainingToolCall--;
    }

    public void decrementRemainingIterations(){
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

    public void soutLoopLimits() {
        System.out.println("while futás szám: " + this.iterations);
        System.out.println("loop határ száma: " + this.remainingIterations);
        System.out.println("tool calling határ száma: " + this.remainingToolCall);
    }
}
