import { Routes } from '@angular/router';

export const routes: Routes = [

  {
    path: '',
    loadComponent: () =>
      import('./home/home').then(m => m.Home),
  },

  {
    path: 'chat/:conversationId',
    loadComponent: () =>
      import('./chat/chat').then(m => m.Chat),
  },

  {
    path: '**',
    redirectTo: ''
  }

];
