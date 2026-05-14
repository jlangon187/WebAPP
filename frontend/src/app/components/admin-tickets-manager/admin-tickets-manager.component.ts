import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-admin-tickets-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-tickets-manager.component.html'
})
export class AdminTicketsManagerComponent implements OnInit {
  tickets: any[] = [];
  loading = true;
  error = '';
  searchTerm = '';
  statusFilter = 'all';
  periodFilter = 'all';
  replyDrafts: { [ticketId: number]: string } = {};

  constructor(private http: HttpClient, private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets() {
    this.loading = true;
    const headers = new HttpHeaders().set('Authorization', `Bearer ${this.authService.getToken()}`);
    this.http.get<any[]>('/api/admin/tickets', { headers }).subscribe({
      next: (tickets) => {
        this.tickets = tickets || [];
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los tickets.';
        this.loading = false;
      }
    });
  }

  get filteredTickets(): any[] {
    const term = this.searchTerm.trim().toLowerCase();
    const now = new Date().getTime();

    return this.tickets.filter((ticket) => {
      const email = (ticket?.usuario?.email || '').toLowerCase();
      const message = (ticket?.mensaje || '').toLowerCase();
      const id = String(ticket?.id || '');
      const status = (ticket?.estado || '').toLowerCase();

      const matchSearch = !term || email.includes(term) || message.includes(term) || id.includes(term);
      const matchStatus = this.statusFilter === 'all' || status === this.statusFilter;

      let matchPeriod = true;
      if (this.periodFilter !== 'all' && ticket?.creadoEn) {
        const created = new Date(ticket.creadoEn).getTime();
        const days = this.periodFilter === '7' ? 7 : this.periodFilter === '30' ? 30 : 90;
        matchPeriod = !Number.isNaN(created) && (now - created) <= days * 24 * 60 * 60 * 1000;
      }

      return matchSearch && matchStatus && matchPeriod;
    });
  }

  replyTicket(ticketId: number) {
    const respuesta = (this.replyDrafts[ticketId] || '').trim();
    if (!respuesta) {
      alert('Debes escribir una respuesta.');
      return;
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${this.authService.getToken()}`);
    this.http.put(`/api/admin/tickets/${ticketId}/responder`, { respuesta }, { headers }).subscribe({
      next: () => {
        this.replyDrafts[ticketId] = '';
        this.loadTickets();
      },
      error: (err) => alert(err?.error || 'No se pudo responder el ticket.')
    });
  }

  closeTicketByAdmin(ticketId: number) {
    const headers = new HttpHeaders().set('Authorization', `Bearer ${this.authService.getToken()}`);
    this.http.put(`/api/admin/tickets/${ticketId}/cerrar`, {}, { headers }).subscribe({
      next: () => this.loadTickets(),
      error: (err) => alert(err?.error || 'No se pudo cerrar el ticket.')
    });
  }

  goBack() {
    this.router.navigate(['/admin']);
  }
}
