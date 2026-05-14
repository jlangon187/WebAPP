import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-admin-encryption-jobs-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-encryption-jobs-manager.component.html'
})
export class AdminEncryptionJobsManagerComponent implements OnInit {
  loading = true;
  error = '';
  searchTerm = '';
  statusFilter = 'all';
  notifyFilter = 'all';

  overview: any = {
    pending: 0,
    running: 0,
    done: 0,
    failed: 0,
    doneWithoutNotification: 0,
    failedWithError: 0,
    mailConfigured: false,
    mailHost: '-',
    recent: []
  };

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    this.loading = true;
    this.error = '';
    const token = this.authService.getToken();
    if (!token) {
      this.router.navigate(['/login']);
      return;
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    this.http.get('/api/admin/encryption-jobs/overview', { headers }).subscribe({
      next: (data: any) => {
        this.overview = data || this.overview;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar la cola de cifrado.';
        this.loading = false;
      }
    });
  }

  get filteredJobs(): any[] {
    const term = this.searchTerm.trim().toLowerCase();
    const rows = this.overview?.recent || [];
    return rows.filter((job: any) => {
      const status = String(job?.status || '').toLowerCase();
      const mod = String(job?.mod || '').toLowerCase();
      const user = String(job?.userEmail || '').toLowerCase();
      const guid = String(job?.guid || '').toLowerCase();
      const id = String(job?.id || '');
      const errorMessage = String(job?.errorMessage || '').toLowerCase();

      const matchSearch = !term ||
        id.includes(term) ||
        mod.includes(term) ||
        user.includes(term) ||
        guid.includes(term) ||
        errorMessage.includes(term);

      const matchStatus = this.statusFilter === 'all' || status === this.statusFilter;
      const matchNotify = this.notifyFilter === 'all' ||
        (this.notifyFilter === 'notified' && !!job?.notifiedAt) ||
        (this.notifyFilter === 'pending' && !job?.notifiedAt);

      return matchSearch && matchStatus && matchNotify;
    });
  }

  statusClass(status: string): string {
    const value = String(status || '').toUpperCase();
    if (value === 'DONE') return 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30';
    if (value === 'RUNNING') return 'bg-blue-500/20 text-blue-300 border border-blue-500/30';
    if (value === 'PENDING') return 'bg-amber-500/20 text-amber-300 border border-amber-500/30';
    return 'bg-red-500/20 text-red-300 border border-red-500/30';
  }

  goBack(): void {
    this.router.navigate(['/admin']);
  }
}
