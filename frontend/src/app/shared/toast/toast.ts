import { Component, inject } from '@angular/core';

import { NotificationService } from '../../core/services/notification.service';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-toast',
  imports: [CommonModule, MatIconModule],
  templateUrl: './toast.html',
  styleUrl: './toast.css',
})
export class Toast {
  private readonly notificationService = inject(NotificationService);
  readonly notificacion = this.notificationService.notificacion;
}
