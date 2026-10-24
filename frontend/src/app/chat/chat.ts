import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { OllamaChatService } from '../service/ollama-chat.service';

type ChatRole = 'user' | 'assistant';

interface ChatMessage {
  id: number;
  role: ChatRole;
  content: string;
}

@Component({
  selector: 'app-chat',
  imports: [FormsModule],
  templateUrl: './chat.html',
  styleUrl: './chat.css',
})
export class Chat {
  private readonly chatService = inject(OllamaChatService);
  private nextId = 1;

  @ViewChild('messageList') private messageList?: ElementRef<HTMLElement>;

  readonly draft = signal('');
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly messages = signal<ChatMessage[]>([
    { id: this.nextId++, role: 'assistant', content: 'Hallo! Ich bin CORTIS. Wie kann ich dir heute helfen?' },
  ]);

  sendMessage(): void {
    const message = this.draft().trim();
    if (!message || this.isLoading()) return;

    const newUserMessage: ChatMessage = { id: this.nextId++, role: 'user', content: message };
    this.messages.update(items => [...items, newUserMessage]);
    this.draft.set('');
    this.errorMessage.set('');
    this.isLoading.set(true);
    this.scrollToBottom();

    this.chatService.sendMessage(message)
      .pipe(finalize(() => {
        this.isLoading.set(false);
        this.scrollToBottom();
      }))
      .subscribe({
        next: response => {
          this.messages.update(items => [
            ...items,
            { id: this.nextId++, role: 'assistant', content: response.message },
          ]);
          this.scrollToBottom();
        },
        error: error => {
          console.error('CORTIS API hiba:', error);
          this.errorMessage.set('Nem sikerült választ kapni a CORTIS backendtől.');
          this.messages.update(items => items.filter(item => item.id !== newUserMessage.id));
          this.draft.set(message);
        },
      });
  }

  onInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    requestAnimationFrame(() => {
      const element = this.messageList?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }
}
