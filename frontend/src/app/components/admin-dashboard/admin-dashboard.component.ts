import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth/auth.service';
import { HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  currentUser: any = null;
  stats: any = {
    totalSales: 0,
    newUsers: 0,
    activeTickets: 0,
    salesTrendPercent: 0,
    usersTrendPercent: 0,
    ticketsTrendPercent: 0,
    nas: {
      online: false,
      usedBytes: 0,
      totalBytes: 0,
      usagePercent: 0,
      homeImagesCount: 0,
      modsFilesCount: 0,
      homeImagesPath: '-',
      modsFilesPath: '-'
    }
  };
  encryptionOverview: any = {
    pending: 0,
    running: 0,
    done: 0,
    failed: 0,
    doneWithoutNotification: 0,
    mailConfigured: false
  };
  loading = true;
  error = '';

  constructor(
      private http: HttpClient, 
      private authService: AuthService, 
      private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();

    const token = this.authService.getToken();
    if(!token) {
        this.router.navigate(['/login']);
        return;
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    this.http.get('/api/admin/stats', { headers }).subscribe({
        next: (res: any) => {
            this.stats = res;
            this.http.get('/api/admin/encryption-jobs/overview', { headers }).subscribe({
              next: (overview: any) => {
                this.encryptionOverview = overview || this.encryptionOverview;
                this.loading = false;
              },
              error: () => {
                this.loading = false;
              }
            });
        },
        error: (err) => {
            this.error = 'No tienes permiso para acceder al panel de administración o la sesión expiró.';
            this.loading = false;
        }
    });
  }

  goToProfileEdit(): void {
    this.router.navigate(['/dashboard']);
  }

  getCurrentGuid(): string {
    const guid = (this.currentUser?.guid || '').trim().toUpperCase();
    return guid || 'No configurado';
  }

  isGuidValid(): boolean {
    const guid = (this.currentUser?.guid || '').trim().toUpperCase();
    return /^[A-F0-9]{18}$/.test(guid);
  }

  goToModsManager() {
    this.router.navigate(['/admin/mods']);
  }

  goToTicketsManager() {
    this.router.navigate(['/admin/tickets']);
  }

  goToUsersManager() {
    this.router.navigate(['/admin/users']);
  }

  formatBytes(value: number): string {
    if (!value || value <= 0) {
      return '0 B';
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
    let size = value;
    let unitIndex = 0;

    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }

    const decimals = size >= 10 ? 1 : 2;
    return `${size.toFixed(decimals)} ${units[unitIndex]}`;
  }

  nasUsageWidth(): string {
    const percent = this.stats?.nas?.usagePercent ?? 0;
    const safe = Math.max(0, Math.min(100, percent));
    return `${safe}%`;
  }

  trendClass(value: number, inverse = false): string {
    if (value === 0) {
      return 'text-slate-400';
    }

    const isPositive = value > 0;
    if (inverse) {
      return isPositive ? 'text-red-500' : 'text-emerald-500';
    }

    return isPositive ? 'text-emerald-500' : 'text-red-500';
  }

  trendIcon(value: number): string {
    if (value === 0) {
      return 'remove';
    }
    return value > 0 ? 'arrow_upward' : 'arrow_downward';
  }

  trendLabel(value: number): string {
    const abs = Math.abs(value || 0);
    return `${abs}%`;
  }

}
