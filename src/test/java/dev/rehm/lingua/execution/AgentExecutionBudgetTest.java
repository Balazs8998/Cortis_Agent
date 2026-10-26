package dev.rehm.lingua.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentExecutionBudgetTest {

    private AgentExecutionBudget agentExecutionBudget;





    @Test
    void decrementRemainingTools_shouldDecrementRemainingToolCallsByOne() {
        this.agentExecutionBudget = new AgentExecutionBudget(2);

        agentExecutionBudget.decrementRemainingTools();
        assertEquals(1, agentExecutionBudget.getRemainingToolCall());
    }

    @Test
    void decrementRemainingTools_shouldReachZeroAfterTwoDecrements() {
        this.agentExecutionBudget = new AgentExecutionBudget(2);

        agentExecutionBudget.decrementRemainingTools();
        agentExecutionBudget.decrementRemainingTools();
        assertEquals(0, agentExecutionBudget.getRemainingToolCall());
    }

    @Test
    void decrementRemainingTools_shouldAllowRemainingToolCallsToBecomeNegative() {
        this.agentExecutionBudget = new AgentExecutionBudget(0);

        agentExecutionBudget.decrementRemainingTools();
        assertEquals(-1, agentExecutionBudget.getRemainingToolCall());
    }

    @Test
    void decrementRemainingIterations_shouldDecrementRemainingIterationsByOne() {
        this.agentExecutionBudget = new AgentExecutionBudget(0,2);

        agentExecutionBudget.decrementRemainingIterations();
        assertEquals(1, agentExecutionBudget.getRemainingIterations());
    }

    @Test
    void decrementRemainingIterations_shouldReachZeroAfterTwoRemainingIterationsDecrements() {
        this.agentExecutionBudget = new AgentExecutionBudget(0,2);

        agentExecutionBudget.decrementRemainingIterations();
        agentExecutionBudget.decrementRemainingIterations();
        assertEquals(0, agentExecutionBudget.getRemainingIterations());
    }

    @Test
    void decrementRemainingIterations_shouldAllowRemainingIterationsToBecomeNegative() {
        this.agentExecutionBudget = new AgentExecutionBudget(0,0);

        agentExecutionBudget.decrementRemainingIterations();
        assertEquals(-1, agentExecutionBudget.getRemainingIterations());
    }

    @Test
    void  incrementIterations_shouldIncrementIterationCountByOne(){
        this.agentExecutionBudget = new AgentExecutionBudget(0,0);

        agentExecutionBudget.incrementIterations();
        assertEquals(1, agentExecutionBudget.getIterations());
    }

    @Test
    void  incrementIterations_shouldIncrementIterationCountTwice(){
        this.agentExecutionBudget = new AgentExecutionBudget(0,0);

        agentExecutionBudget.incrementIterations();
        agentExecutionBudget.incrementIterations();
        assertEquals(2, agentExecutionBudget.getIterations());
    }

}