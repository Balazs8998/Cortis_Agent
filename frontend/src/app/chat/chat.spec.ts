import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { Chat } from './chat';
import { OllamaChatService } from '../service/ollama-chat.service';

describe('Chat workspace', () => {
  const id = '12345678-1234-4234-8234-123456789abc';
  let response: Subject<{ message: string }>;
  let component: Chat;
  let calls: unknown[][];
  beforeEach(() => {
    response = new Subject();
    calls = [];
    TestBed.configureTestingModule({
      imports: [Chat],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => id } } } },
        {
          provide: OllamaChatService,
          useValue: {
            sendMessage: (...args: unknown[]) => {
              calls.push(args);
              return response;
            },
          },
        },
      ],
    });
    component = TestBed.createComponent(Chat).componentInstance;
  });
  it('sends the existing API contract and records the real response', () => {
    component.draft.set('  24 + 18  ');
    component.sendMessage();
    component.sendMessage();
    expect(calls).toEqual([[id, '24 + 18']]);
    expect(component.selected()?.status).toBe('Running');
    response.next({ message: '42' });
    response.complete();
    expect(component.messages().at(-1)?.content).toBe('42');
    expect(component.selected()?.response).toEqual({ message: '42' });
    expect(component.selected()?.status).toBe('Completed');
    expect(component.isLoading()).toBe(false);
  });
  it('keeps failed attempts visible and restores the draft', () => {
    component.draft.set('hello');
    component.sendMessage();
    response.error({ status: 500, error: 'failed' });
    expect(component.selected()?.status).toBe('Failed');
    expect(component.messages()[0].failed).toBe(true);
    expect(component.draft()).toBe('hello');
    expect(component.isLoading()).toBe(false);
  });
  it('does not submit whitespace', () => {
    component.draft.set('   ');
    component.sendMessage();
    expect(calls.length).toBe(0);
  });
});
