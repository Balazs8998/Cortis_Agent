import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { Chat } from './chat';
import { ChatResponse, OllamaChatService } from '../service/ollama-chat.service';

describe('Chat workspace', () => {
  const id = '12345678-1234-4234-8234-123456789abc';
  let response: Subject<Partial<ChatResponse>>;
  let fixture: ComponentFixture<Chat>;
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
    fixture = TestBed.createComponent(Chat);
    component = fixture.componentInstance;
  });
  it('sends the existing API contract and records the real response', () => {
    component.draft.set('  24 + 18  ');
    component.sendMessage();
    component.sendMessage();
    expect(calls).toEqual([[id, '24 + 18', 'qwen3.5:4b', true]]);
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
  it('sends normal GPT-OSS as boolean false and preserves each request profile', () => {
    component.model.set('gpt-oss:20b');
    component.think.set(false);
    component.draft.set('hello');
    component.sendMessage();
    expect(calls[0]).toEqual([id, 'hello', 'gpt-oss:20b', false]);
    component.model.set('qwen3:8b');
    component.think.set(true);
    expect(component.selected()?.request).toEqual({
      conversationId: id,
      message: 'hello',
      model: 'gpt-oss:20b',
      think: false,
    });
  });
  it('renders selected response issues and done reason without using the latest request', () => {
    component.draft.set('first');
    component.sendMessage();
    const firstId = component.selectedId();
    response.next({
      message: 'Partial answer',
      agentTerminationReason: 'COMPLETED',
      agentResponseIssues: ['MODEL_OUTPUT_TRUNCATED'],
      doneReason: 'length',
    });
    response.complete();
    expect(component.selected()?.status).toBe('Warning');
    expect(component.messages().at(-1)?.issues).toEqual(['MODEL_OUTPUT_TRUNCATED']);
    response = new Subject();
    component.draft.set('second');
    component.sendMessage();
    response.error({ status: 500, error: 'latest failure' });
    component.selectedId.set(firstId);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Csonkolt válasz');
    expect(fixture.nativeElement.textContent).toContain('length');
    expect(fixture.nativeElement.querySelector('.inspector-content').textContent).not.toContain(
      'latest failure',
    );
    expect(component.selectedIssues()).toEqual(['MODEL_OUTPUT_TRUNCATED']);
  });
  it('displays empty and unknown issues and deduplicates truncation warnings', () => {
    component.draft.set('hello');
    component.sendMessage();
    response.next({
      message: '',
      agentTerminationReason: 'COMPLETED',
      doneReason: 'length',
      agentResponseIssues: ['MODEL_OUTPUT_TRUNCATED', 'FUTURE_ISSUE'],
    });
    response.complete();
    fixture.detectChanges();
    expect(component.selectedIssues()).toEqual([
      'MODEL_OUTPUT_TRUNCATED',
      'FUTURE_ISSUE',
      'MODEL_RESPONSE_EMPTY',
    ]);
    expect(fixture.nativeElement.textContent).toContain('Üres válasz');
    expect(fixture.nativeElement.textContent).toContain('FUTURE_ISSUE');
  });
  it('binds model and mode selectors and disables them during a request', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const model = fixture.nativeElement.querySelector('[aria-label="Modell"]') as HTMLSelectElement;
    model.value = model.options[2].value;
    model.dispatchEvent(new Event('change'));
    const mode = fixture.nativeElement.querySelector(
      '[aria-label="Működési profil"]',
    ) as HTMLSelectElement;
    mode.value = mode.options[1].value;
    mode.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.model()).toBe('gpt-oss:20b');
    expect(component.think()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('low gondolkodási szint');
    component.draft.set('test');
    component.sendMessage();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    response.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(false);
  });
  it('marks limit termination as a warning even with a nonempty answer', () => {
    component.draft.set('hello');
    component.sendMessage();
    response.next({ message: 'Partial result', agentTerminationReason: 'TOOL_LIMIT_REACHED' });
    expect(component.selected()?.status).toBe('Warning');
  });
  it('does not submit whitespace', () => {
    component.draft.set('   ');
    component.sendMessage();
    expect(calls.length).toBe(0);
  });
});
