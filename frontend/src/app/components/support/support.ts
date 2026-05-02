import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support.html',
  styleUrl: './support.css',
})
export class Support implements OnInit {
  message = '';
  loading = false;
  successMessage = '';
  errorMessage = '';
  tickets: any[] = [];

  constructor(private http: HttpClient, private authService: AuthService) {}

  ngOnInit(): void {
    this.loadMyTickets();
  }

  submitTicket() {
    this.successMessage = '';
    this.errorMessage = '';

    const payloadMessage = this.message.trim();
    if (!payloadMessage) {
      this.errorMessage = 'Debes escribir el detalle del problema.';
      return;
    }

    this.loading = true;
    this.http.post('/api/tickets', { mensaje: payloadMessage }, { headers: this.getAuthHeaders() }).subscribe({
      next: () => {
        this.loading = false;
        this.message = '';
        this.successMessage = 'Ticket enviado correctamente.';
        this.loadMyTickets();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error || 'No se pudo crear el ticket.';
      }
    });
  }

  loadMyTickets() {
    this.http.get<any[]>('/api/tickets/mis-tickets', { headers: this.getAuthHeaders() }).subscribe({
      next: (data) => {
        this.tickets = data || [];
      },
      error: () => {
        this.tickets = [];
      }
    });
  }

  closeTicket(ticketId: number) {
    this.http.put(`/api/tickets/${ticketId}/cerrar`, {}, { headers: this.getAuthHeaders() }).subscribe({
      next: () => this.loadMyTickets(),
      error: (err) => {
        this.errorMessage = err?.error || 'No se pudo cerrar el ticket.';
      }
    });
  }

  getTicketSummary(message: string): string {
    if (!message) {
      return '';
    }
    const splitIndex = message.indexOf('\n\n--- RESPUESTA SOPORTE ---\n');
    const base = splitIndex >= 0 ? message.substring(0, splitIndex) : message;
    return base.length > 140 ? `${base.substring(0, 140)}...` : base;
  }

  hasSupportReply(message: string): boolean {
    return !!message && message.includes('--- RESPUESTA SOPORTE ---');
  }

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }
}
