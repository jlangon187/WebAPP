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
  stats: any = {
    totalSales: 0,
    totalSalesCount: 0,
    salesCountLast30: 0,
    totalUsers: 0,
    newUsers: 0,
    totalTickets: 0,
    activeTickets: 0,
    closedTickets: 0,
    respondedTickets: 0,
    openTickets: 0,
    totalMods: 0,
    featuredMods: 0,
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
  downloadingApk = false;

  constructor(
      private http: HttpClient, 
      private authService: AuthService, 
      private router: Router
  ) {}

  ngOnInit(): void {
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
    this.router.navigate(['/dashboard'], { queryParams: { edit: '1' } });
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

  downloadAdminApk(): void {
    const token = this.authService.getToken();
    if (!token || this.downloadingApk) {
      return;
    }

    this.downloadingApk = true;
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    this.http.get('/api/admin/apk-download', { headers, responseType: 'blob', observe: 'response' }).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.error = 'No se pudo descargar el APK.';
          this.downloadingApk = false;
          return;
        }

        const disposition = response.headers.get('content-disposition') || '';
        const match = disposition.match(/filename="?([^";]+)"?/i);
        const fileName = match?.[1] || 'gpb-admin.apk';

        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = fileName;
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.downloadingApk = false;
      },
      error: (err) => {
        this.error = typeof err?.error === 'string' ? err.error : 'No se pudo descargar el APK del panel admin.';
        this.downloadingApk = false;
      }
    });
  }

  goToEncryptionJobsManager() {
    this.router.navigate(['/admin/encryption-jobs']);
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

  kpiCards(): Array<{ title: string; value: string | number; sub: string; icon: string; trend?: number; inverseTrend?: boolean }> {
    return [
      {
        title: 'Ventas (EUR)',
        value: `${Number(this.stats.totalSales || 0).toLocaleString()} EUR`,
        sub: `${this.stats.totalSalesCount || 0} compras totales`,
        icon: 'payments',
        trend: this.stats.salesTrendPercent
      },
      {
        title: 'Usuarios',
        value: this.stats.totalUsers || 0,
        sub: `${this.stats.newUsers || 0} nuevos en 30 dias`,
        icon: 'group',
        trend: this.stats.usersTrendPercent
      },
      {
        title: 'Tickets',
        value: this.stats.totalTickets || 0,
        sub: `${this.stats.activeTickets || 0} activos | ${this.stats.closedTickets || 0} cerrados`,
        icon: 'confirmation_number',
        trend: this.stats.ticketsTrendPercent,
        inverseTrend: true
      },
      {
        title: 'Mods',
        value: this.stats.totalMods || 0,
        sub: `${this.stats.featuredMods || 0} en showroom`,
        icon: 'inventory_2'
      }
    ];
  }

}
