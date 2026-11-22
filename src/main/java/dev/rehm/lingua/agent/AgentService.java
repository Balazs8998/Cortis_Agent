package dev.rehm.lingua.agent;

import java.util.concurrent.*;

import dev.rehm.lingua.agent.context.AgentExecutionContext;
import dev.rehm.lingua.agent.inspection.AgentExecutionResult;
import dev.rehm.lingua.agent.inspection.AgentResponseIssue;
import dev.rehm.lingua.agent.inspection.AgentTerminationReason;
import dev.rehm.lingua.common.exception.classes.*;
import dev.rehm.lingua.conversation.ConversationService;
import dev.rehm.lingua.agent.message.MessageService;
import dev.rehm.lingua.llm.LlmProfileService;
import dev.rehm.lingua.llm.LlmProfiles;
import dev.rehm.lingua.chat.dto.ChatRequest;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.agent.message.MessageFactory;
import dev.rehm.lingua.llm.ollama.RequestFactory;
import dev.rehm.lingua.llm.ollama.OllamaService;
import dev.rehm.lingua.tool.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;


@Service
public class AgentService {

    private final ToolExecutorService executorService;
    private final OllamaService ollamaService;
    private final MessageService messageService;
    private final RequestFactory requestFactory;
    private final ConversationService conversationService;
    private final LlmProfileService llmProfileService;


    public AgentService(MessageService messageService, ToolExecutorService executorService, OllamaService ollamaService, RequestFactory requestFactory, ConversationService conversationService, LlmProfileService llmProfileService) {

        this.messageService = messageService;
        this.executorService = executorService;
        this.ollamaService = ollamaService;
        this.requestFactory = requestFactory;

        this.conversationService = conversationService;
        this.llmProfileService = llmProfileService;

    }

    public AgentExecutionResult executeAgentLoop(
            ChatRequest request
    ) {

        logLock("WAITING", request.conversationId());

        return conversationService.executeSerialized(
                request.conversationId(),
                () -> {
                    logLock("ACQUIRED", request.conversationId());

                    try {
                        return runAgentLoop(request);

                    } finally {
                        logLock("FINISHED", request.conversationId());
                    }
                }
        );
    }

    private AgentExecutionResult runAgentLoop(
            ChatRequest request
    ) {
        AgentExecutionContext context =
                new AgentExecutionContext(request, choiceLlm(request));
        context.setCommittedHistory(conversationService.getMessageList(context.getUuid()));


        logIterationBudget(context);

        while (context.getHasToolCalling()) {



            if (context.getAgentExecutionBudget().getRemainingIterations() <= 0) {
                AgentTerminationReason reason = AgentTerminationReason.ITERATION_LIMIT_REACHED;
                return finishAfterLimit(reason, context);
            }

            executeAgentIteration(context);


            if (context.getHasToolCalling()) {
                context.addToolResults(executorService.toolCallingProcess(context));


                for (ToolExecutionResult toolExecutionResult : context.getResults()) {

                    if (toolExecutionResult.status() == ToolExecutionStatus.SKIPPED) {
                        AgentTerminationReason reason = AgentTerminationReason.TOOL_LIMIT_REACHED;
                        return finishAfterLimit(reason, context);
                    }
                }
            }
        }


        conversationService.messageSave(context.getUuid(), context.getPendingMessages());
        conversationService.soutMessageList(conversationService.getMessageList(context.getUuid()));
        logIterationBudget(context);

        return createExecutionResult(AgentTerminationReason.COMPLETED, context);
    }



    private LlmProfiles choiceLlm(ChatRequest request) {
        String model = request.model();

        if (model == null || model.isBlank()) {
            model = "qwen3.5:4b";
        }

        boolean think = request.think() == null || request.think();

        return llmProfileService.createLlmProfiles(model, think);
    }


    private void retryEmptyFinalResponse(AgentExecutionContext context) {


        if (!hasToolCalling(context.getResponse())
                && (context.getResponse().message().content() == null
                || context.getResponse().message().content().isBlank())) {

            System.out.println("Empty final response. Retrying without tools.");

            context.setResponse(agentMessage(
                    requestFactory.createBaseRequest(context.getMessagesForLlm(), context.getLlmProfiles().finalProfile())));

            validateFinalResponse(context);
        }

    }

    private void executeAgentIteration(AgentExecutionContext context) {
        OllamaChatRequest ollamaChatRequestWithTool = requestFactory.createRequestWithTool(context.getMessagesForLlm(), context.getLlmProfiles().agentProfile());

        context.getAgentExecutionBudget().loopStep();

        context.setResponse(agentMessage(ollamaChatRequestWithTool));

        retryEmptyFinalResponse(context);

        context.addPendingMessage(context.getResponse().message());
        context.setHasToolCalling(hasToolCalling(context.getResponse()));
    }


    private OllamaChatResponse agentMessage(OllamaChatRequest request) {

        OllamaChatResponse response = ollamaService.messageSend(request);


        if (response == null) {
            throw new UnexpectedNullValueException(
                    "OllamaResponse",
                    "response",
                    null,
                    "Ollama returned a null response.",
                    "OLLAMA_RESPONSE_NULL"
            );
        }

        if (response.message() == null) {
            throw new UnexpectedNullValueException(
                    "OllamaResponse",
                    "message",
                    null,
                    "Ollama response does not contain a message.",
                    "OLLAMA_MESSAGE_NULL"
            );
        }

        if(!response.done()){
            throw new AgentProcessFailedException(
                    "addMessage",
                    "boolean done",
                    false
            );

        }

        if (!"assistant".equals(response.message().role())) {
            throw new AgentInvalidMessageRoleException(
                    "agentMessage", "role", response.message().role()
            );
        }



        System.out.printf(
                "OLLAMA DEBUG | done=%s | reason=%s | eval=%s | content='%s' | toolCalls=%s%n",
                response.done(),
                response.doneReason(),
                response.evalCount(),
                response.message().content(),
                response.message().toolCalls()
        );

        return response;

    }

    private AgentExecutionResult finishAfterLimit(AgentTerminationReason reason,
                                                  AgentExecutionContext context) {

        String message = reason == AgentTerminationReason.ITERATION_LIMIT_REACHED
                ? "Agent iteration limit reached. "
                : "Agent tool limit reached. ";

        context.addPendingMessage(MessageFactory.createSystemMessageDto(
                message + "Report confirmed results and identify incomplete tasks."
        ));

        OllamaChatRequest ollamaChatRequest = requestFactory.createBaseRequest(context.getMessagesForLlm(), context.getLlmProfiles().finalProfile());

        context.setResponse(agentMessage(ollamaChatRequest));


        context.addPendingMessage(context.getResponse().message());

        validateFinalResponse(context);

        conversationService.messageSave(context.getUuid(), context.getPendingMessages());

        conversationService.soutMessageList(conversationService.getMessageList(context.getUuid()));
        logIterationBudget(context);
        System.out.println("-------end-----------");


        return createExecutionResult(reason, context);
    }

    private void validateFinalResponse(AgentExecutionContext context){

        OllamaChatResponse response = context.getResponse();

        if (hasToolCalling(response)) {
            throw new AgentUnexpectedFinalToolCallsException(
                    "finishAfterLimit",
                    "toolCalls",
                    response.message().toolCalls().size()
            );
        }

        String content = response.message().content();

        if (content == null || content.isBlank()) {
            throw new AgentEmptyFinalResponseException(
                    "finishAfterLimit",
                    "content",
                    content
            );
        }

    }

    private AgentExecutionResult createExecutionResult(AgentTerminationReason agentTerminationReason,AgentExecutionContext context) {

        int successful = 0;
        int failed = 0;

        int toolCalls = context.getResults().size();
        for (ToolExecutionResult toolExecutionResult : context.getResults()) {
            if (toolExecutionResult.status() == ToolExecutionStatus.SUCCES) {
                successful += 1;
            }
            if (toolExecutionResult.status() == ToolExecutionStatus.FAILED
                    || toolExecutionResult.status() == ToolExecutionStatus.SKIPPED
                    || toolExecutionResult.status() == ToolExecutionStatus.REJECTED
                    || toolExecutionResult.status() == ToolExecutionStatus.TIMEOUT) {
                failed += 1;
            }


        }

        Set<AgentResponseIssue> issues =
                EnumSet.noneOf(AgentResponseIssue.class);

        if ("length".equals(context.getResponse().doneReason())) {
            issues.add(AgentResponseIssue.MODEL_OUTPUT_TRUNCATED);
        }

        if (context.getResponse().message().content() == null
                || context.getResponse().message().content().isBlank()) {
            issues.add(AgentResponseIssue.MODEL_RESPONSE_EMPTY);
        }

        return new AgentExecutionResult(
                context.getResponse(),
                context.getAgentExecutionBudget().getRemainingToolCall(),
                context.getAgentExecutionBudget().getRemainingIterations(),
                context.getResults(),
                toolCalls,
                successful,
                failed,
                agentTerminationReason,
                issues
        );
    }



    private @NotNull boolean hasToolCalling(OllamaChatResponse response) {
        if (response.message().toolCalls() == null) {
            return false;
        }
        return !response.message().toolCalls().isEmpty();
    }

    private void logLock(String status, UUID uuid) {
        System.out.printf(
                "%s | UUID: %s | Thread: %d | Time: %s%n",
                status,
                uuid,
                Thread.currentThread().threadId(),
                Instant.now()
        );
    }

    private void logIterationBudget(AgentExecutionContext context) {
        System.out.println("------------------");
        context.getAgentExecutionBudget().printLoopLimits();
        System.out.println("------------------");

    }

}
