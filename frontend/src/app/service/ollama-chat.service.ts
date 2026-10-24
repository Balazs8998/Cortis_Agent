import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

interface ChatRequest {
  message: string;
}

interface ChatResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class OllamaChatService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/ollama/chat';

  sendMessage(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.url, { message } satisfies ChatRequest);
  }
}
