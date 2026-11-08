import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

interface ChatRequest {
  conversationId: string;
  message: string;
}

export interface ChatResponse {
  message: string;
  remainingToolCalls: number;
  remainingIterations: number;
  toolExecutionResults: ToolExecutionResult[];
  toolCalls: number;
  successful: number;
  failed: number;
  agentTerminationReason: AgentTerminationReason;
}

interface CreateConversationResponse {
  id: string;
}

export type AgentTerminationReason =
  | 'COMPLETED'
  | 'ITERATION_LIMIT_REACHED'
  | 'TOOL_LIMIT_REACHED';

export type ToolExecutionStatus =
  | 'SUCCESS'
  | 'FAILED'
  | 'SKIPPED'
  | 'REJECTED';

export interface ToolExecutionResult {
  toolName: string;
  arguments: Record<string, unknown>;
  result: unknown;
  status: ToolExecutionStatus;
  executedAt: string;
  durationMs: number;
}

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function isConversationId(value: string): boolean {
  return UUID_PATTERN.test(value);
}

@Injectable({
  providedIn: 'root'
})
export class OllamaChatService {

  private readonly http = inject(HttpClient);

  private readonly chatUrl = '/api/ollama/chat';

  private readonly conversationUrl =
    '/api/ollama/conversations';

  createConversation(): Observable<string> {
    return this.http.post(
      this.conversationUrl,
      {},
      { responseType: 'text' }
    ).pipe(
      map(response => {
        const conversationId = response.trim();

        if (!isConversationId(conversationId)) {
          throw new Error('Invalid conversation UUID');
        }

        return conversationId;
      })
    );
  }

  sendMessage(
    conversationId: string,
    message: string
  ): Observable<ChatResponse> {
    const request: ChatRequest = {
      conversationId,
      message
    };

    return this.http.post<ChatResponse>(
      this.chatUrl,
      request
    );
  }
}
