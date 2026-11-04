
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { OllamaChatService } from '../service/ollama-chat.service';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {

  private readonly router = inject(Router);
  private readonly chatService = inject(OllamaChatService);

  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  createNewChat(): void {

    if (this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.chatService.createConversation()
      .pipe(
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: conversationId => {
          void this.router.navigate([
            '/chat',
            conversationId
          ]);
        },

        error: error => {
          console.error('Conversation creation failed:', error);

          this.errorMessage.set(
            'Nem sikerült új beszélgetést létrehozni. Ellenőrizd a backend kapcsolatát.'
          );
        }
      });
  }
}
