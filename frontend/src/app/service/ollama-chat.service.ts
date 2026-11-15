import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

export type LlmModel = 'qwen3.5:4b' | 'qwen3:8b' | 'gpt-oss:20b';

export interface ChatRequest {
  conversationId: string;
  message: string;
  model: LlmModel;
  think: boolean;
}

export interface ChatResponse {
  conversationId?: string;
  agentResponseIssues?: string[] | null;
  doneReason?: string | null;
  message: string;
  remainingToolCalls: number;
  remainingIterations: number;
  toolExecutionResults: ToolExecutionResult[];
  toolCalls: number;
  successful: number;
  failed: number;
  agentTerminationReason: AgentTerminationReason;
}

export type AgentTerminationReason = 'COMPLETED' | 'ITERATION_LIMIT_REACHED' | 'TOOL_LIMIT_REACHED';

export type ToolExecutionStatus =
  'SUCCESS' | 'SUCCES' | 'FAILED' | 'SKIPPED' | 'REJECTED' | 'TIMEOUT';

export interface ToolExecutionResult {
  toolName: string;
  arguments: Record<string, unknown>;
  result: unknown;
  status: ToolExecutionStatus;
  executedAt: string;
  durationMs: number;
  startThreadId: number | null;
  startIsVirtual: boolean;
  toolCallIndex: number | null;
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function isConversationId(value: string): boolean {
  return UUID_PATTERN.test(value);
}

@Injectable({
  providedIn: 'root',
})
export class OllamaChatService {
  private readonly http = inject(HttpClient);

  private readonly chatUrl = '/api/ollama/chat';

  private readonly conversationUrl = '/api/ollama/conversations';

  createConversation(): Observable<string> {
    return this.http.post(this.conversationUrl, {}, { responseType: 'text' }).pipe(
      map((response) => {
        const conversationId = response.trim();

        if (!isConversationId(conversationId)) {
          throw new Error('Invalid conversation UUID');
        }

        return conversationId;
      }),
    );
  }

  sendMessage(
    conversationId: string,
    message: string,
    model: LlmModel = 'qwen3.5:4b',
    think: boolean = true,
  ): Observable<ChatResponse> {
    const request: ChatRequest = {
      conversationId,
      message,
      model,
      think,
    };

    return this.http.post<ChatResponse>(this.chatUrl, request);
  }
}
