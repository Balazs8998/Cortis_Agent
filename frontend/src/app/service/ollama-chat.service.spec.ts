import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OllamaChatService } from './ollama-chat.service';

describe('OllamaChatService profile contract', () => {
  let service: OllamaChatService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OllamaChatService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('posts false as a boolean and retains backend diagnostics', () => {
    const response = {
      conversationId: 'conversation',
      message: 'partial',
      agentResponseIssues: ['MODEL_OUTPUT_TRUNCATED'],
      doneReason: 'length',
    };
    service.sendMessage('conversation', 'question', 'gpt-oss:20b', false).subscribe((value) => {
      expect(value).toEqual(response);
    });
    const request = http.expectOne('/api/ollama/chat');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      conversationId: 'conversation',
      message: 'question',
      model: 'gpt-oss:20b',
      think: false,
    });
    request.flush(response);
  });
});
