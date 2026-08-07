import { Component, inject } from '@angular/core';

import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-toast',
  imports: [],
  templateUrl: './toast.html',
  styleUrl: './toast.css',
})
export class Toast {
  private readonly notificationService = inject(NotificationService);
  readonly notificacion = this.notificationService.notificacion;
}
