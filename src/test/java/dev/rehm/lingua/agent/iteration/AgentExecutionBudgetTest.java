package dev.rehm.lingua.agent.iteration;

import dev.rehm.lingua.common.exception.classes.IterationValueException;
import dev.rehm.lingua.common.exception.classes.ToolBudgetException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentExecutionBudgetTest {



    @Test
    void shouldUseDefaultBudgetsWhenBothValuesAreNegative() {
        AgentExecutionBudget budget = new AgentExecutionBudget(-1, -1);

        assertEquals(4, budget.getRemainingToolCall());
        assertEquals(6, budget.getRemainingIterations());
    }

    @Test
    void shouldUseDefaultIterationBudgetWhenNegative() {
        AgentExecutionBudget budget = new AgentExecutionBudget(4, -1);

        assertEquals(4, budget.getRemainingToolCall());
        assertEquals(6, budget.getRemainingIterations());
    }

    @Test
    void shouldUseDefaultBudgetsWhenSingleArgumentIsNegative() {
        AgentExecutionBudget budget = new AgentExecutionBudget(-1);

        assertEquals(4, budget.getRemainingToolCall());
        assertEquals(6, budget.getRemainingIterations());
    }

    @Test
    void shouldDecrementRemainingIterationsWhenBudgetIsAvailable(){
        AgentExecutionBudget budget = new AgentExecutionBudget(2);

        budget.loopStep();

        assertEquals(3, budget.getRemainingIterations());
        assertEquals(1,budget.getIterations());
    }

    @Test
    void shouldSuccessfullyExecuteTwoConsecutiveLoopSteps(){
        AgentExecutionBudget budget = new AgentExecutionBudget(2);

        budget.loopStep();

        assertEquals(3, budget.getRemainingIterations());
        assertEquals(1,budget.getIterations());
        budget.loopStep();
        assertEquals(2, budget.getRemainingIterations());
        assertEquals(2,budget.getIterations());
    }

    @Test
    void shouldDecrementRemainingIterationsFromOneToZero(){
        AgentExecutionBudget budget = new AgentExecutionBudget(2, 1);

        assertDoesNotThrow(budget::loopStep);
        assertEquals(0,budget.getRemainingIterations());
        assertEquals(1,budget.getIterations());
    }

    @Test
    void shouldThrowExceptionWhenRemainingIterationsIsZero(){
        AgentExecutionBudget budget = new AgentExecutionBudget(2, 1);
        assertDoesNotThrow(budget::loopStep);
        assertEquals(1,budget.getIterations());
        assertThrows(IterationValueException.class,budget::loopStep);
        assertEquals(0,budget.getRemainingIterations());
        assertEquals(1,budget.getIterations());

    }

    @Test
    void shouldExecuteLoopStepWithDefaultBudgetAfterNegativeInput(){
        AgentExecutionBudget budget = new AgentExecutionBudget(2, -1);
        assertDoesNotThrow(budget::loopStep);
        assertEquals(5,budget.getRemainingIterations());
        assertEquals(1,budget.getIterations());

    }

    @Test
    void shouldApproveAllToolCallsWhenRequestedIsWithinBudget(){
        int requestedToolCalls = 2;
        AgentExecutionBudget budget = new AgentExecutionBudget(4);

        int approved = assertDoesNotThrow(() -> budget.reserveBudget(requestedToolCalls));
        assertEquals(requestedToolCalls,approved);
        assertEquals(2,budget.getRemainingToolCall());


    }

    @Test
    void shouldApproveOnlyRemainingToolCallsWhenRequestedExceedsBudget(){
        int requestedToolCalls = 6;
        AgentExecutionBudget budget = new AgentExecutionBudget(4);

        int approved = assertDoesNotThrow(() -> budget.reserveBudget(requestedToolCalls));
        assertEquals(4,approved);
        assertEquals(0,budget.getRemainingToolCall());
    }

    @Test
    void shouldThrowExceptionWhenRequestedToolCallsIsNegative(){
        int requestedToolCalls = -2;
        AgentExecutionBudget budget = new AgentExecutionBudget(4);

        assertThrows(
                ToolBudgetException.class,
                () -> budget.reserveBudget(requestedToolCalls)
        );

        assertEquals(4, budget.getRemainingToolCall());

    }

    @Test
    void shouldReturnZeroWhenRequestedToolCallsIsZero(){
        int requestedToolCalls = 0;
        AgentExecutionBudget budget = new AgentExecutionBudget(4);

        int approved = assertDoesNotThrow(() -> budget.reserveBudget(requestedToolCalls));
        assertEquals(0,approved);
        assertEquals(4,budget.getRemainingToolCall());

    }

    @Test
    void shouldReturnZeroWhenNoToolCallBudgetRemains(){
        int requestedToolCalls = 4;
        AgentExecutionBudget budget = new AgentExecutionBudget(4);

        int approved = assertDoesNotThrow(() -> budget.reserveBudget(requestedToolCalls));
        assertEquals(4,approved);
        assertEquals(0,budget.getRemainingToolCall());

        int approved2 = assertDoesNotThrow(() -> budget.reserveBudget(requestedToolCalls));
        assertEquals(0,approved2);
        assertEquals(0,budget.getRemainingToolCall());
    }
}