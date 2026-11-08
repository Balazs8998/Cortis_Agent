import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, JsonPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  OllamaChatService,
  isConversationId,
  ChatResponse
} from '../service/ollama-chat.service';
import { Panel } from '../workspace/panel';

interface Message {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  time: number;
  failed?: boolean;
}
interface Run {
  id: number;
  time: number;
  status: 'Running' | 'Completed' | 'Failed';
  duration: number | null;
  request: unknown;
  response?: ChatResponse;
  remainingToolCalls?: number;
  remainingIterations?: number;
  error?: unknown;
}
@Component({
  selector: 'app-chat',
  imports: [FormsModule, DatePipe, JsonPipe, Panel],
  templateUrl: './chat.html',
  styleUrls: ['./chat.css', './inspector.css'],
})
export class Chat implements AfterViewInit {
  private service = inject(OllamaChatService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  @ViewChild('chatPanel') chatPanel!: Panel;
  @ViewChild('inspectorPanel') inspectorPanel!: Panel;
  @ViewChild('desktop') desktop!: ElementRef<HTMLElement>;
  @ViewChild('messageList') messageList?: ElementRef<HTMLElement>;
  readonly conversationId = signal('');
  readonly draft = signal('');
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly messages = signal<Message[]>([]);
  readonly runs = signal<Run[]>([]);
  readonly selectedId = signal<number | null>(null);
  readonly selected = computed(
    () => this.runs().find((r) => r.id === this.selectedId()) ?? this.runs().at(-1),);
  readonly selectedToolExecutions = computed(() => this.selected()?.response?.toolExecutionResults ?? [],);
  readonly tab = signal('Overview');
  readonly tabs = ['Overview', 'Messages', 'Timeline', 'Tools', 'Raw'];
  readonly role = signal('all');
  readonly filteredMessages = computed(() =>
    this.messages().filter((m) => this.role() === 'all' || m.role === this.role()),
  );
  private nextId = 1;
  constructor() {
    const id = this.route.snapshot.paramMap.get('conversationId');
    if (id && isConversationId(id)) this.conversationId.set(id);
    else void this.router.navigate(['/']);
  }
  ngAfterViewInit() {
    setTimeout(() => this.arrange());
  }
  @HostListener('window:resize') arrange() {
    if (!this.desktop) return;
    const { clientWidth: w, clientHeight: h } = this.desktop.nativeElement;
    const left = Math.round((w - 18) * 0.54);
    this.chatPanel.arrange(0, 0, left, h);
    this.inspectorPanel.arrange(left + 18, 0, w - left - 18, h);
  }
  sendMessage() {
    const message = this.draft().trim(),
      conversationId = this.conversationId();
    if (!message || !conversationId || this.isLoading()) return;
    const id = this.nextId++,
      start = performance.now(),
      time = Date.now();
    this.messages.update((m) => [...m, { id, role: 'user', content: message, time }]);
    this.runs.update((r) => [
      ...r,
      { id, time, status: 'Running', duration: null, request: { conversationId, message } },
    ]);
    this.selectedId.set(id);
    this.draft.set('');
    this.errorMessage.set('');
    this.isLoading.set(true);
    this.scroll();
    this.service
      .sendMessage(conversationId, message)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isLoading.set(false);
          this.scroll();
        }),
      )
      .subscribe({
        next: (response) => {
          this.messages.update((m) => [
            ...m,
            { id: this.nextId++, role: 'assistant', content: response.message, time: Date.now() },
          ]);
          this.updateRun(id, {
            status: 'Completed',
            response,
            duration: Math.round(performance.now() - start),
            remainingToolCalls: response.remainingToolCalls,
            remainingIterations: response.remainingIterations
          });
        },
        error: (error) => {
          this.errorMessage.set(
            'Nem érkezett válasz. A backend már feldolgozhatta az üzenetet; az újraküldés ismétlést okozhat.',
          );
          this.messages.update((m) => m.map((v) => (v.id === id ? { ...v, failed: true } : v)));
          this.draft.set(message);
          this.updateRun(id, {
            status: 'Failed',
            duration: Math.round(performance.now() - start),
            error: {
              status: error.status ?? null,
              message: error.message ?? 'Unknown error',
              body: error.error ?? null,
            },
          });
        },
      });
  }
  private updateRun(id: number, changes: Partial<Run>) {
    this.runs.update((r) => r.map((v) => (v.id === id ? { ...v, ...changes } : v)));
  }
  backToHome() {
    void this.router.navigate(['/']);
  }
  onInputKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
      e.preventDefault();
      this.sendMessage();
    }
  }
  private scroll() {
    requestAnimationFrame(() => {
      const e = this.messageList?.nativeElement;
      if (e) e.scrollTop = e.scrollHeight;
    });
  }
  exportTrace() {
    const blob = new Blob(
      [
        JSON.stringify(
          { conversationId: this.conversationId(), source: 'browser-http', runs: this.runs() },
          null,
          2,
        ),
      ],
      { type: 'application/json' },
    );
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'cortis-trace.json';
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
