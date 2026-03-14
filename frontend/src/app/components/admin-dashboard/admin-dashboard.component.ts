import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth/auth.service';
import { HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Mod, ModService } from '../../services/mod/mod.service';
import { AdminModEditorComponent } from '../admin-mod-editor/admin-mod-editor';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, AdminModEditorComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
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
  loading = true;
  error = '';
  
  mods: Mod[] = [];
  showEditor = false;
  isEditing = false;
  editingMod: Mod | null = null;

  constructor(
      private http: HttpClient, 
      private authService: AuthService, 
      private router: Router,
      private modService: ModService
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
            this.loading = false;
        },
        error: (err) => {
            this.error = 'No tienes permiso para acceder al panel de administración o la sesión expiró.';
            this.loading = false;
        }
    });
    this.loadCatalog();
  }

  loadCatalog() {
      this.modService.getCatalog().subscribe({
          next: (mods) => {
              this.mods = mods;
          },
          error: (err) => console.error("Error cargando mods", err)
      });
  }

  openCreateMod() {
      this.isEditing = false;
      this.editingMod = {
          id: 0,
          nombre: '',
          descripcion: '',
          precio: 0,
          version: '1.0',
          archivoOriginal: '',
          categoria: null,
          destacadoHome: false,
          ordenShowroom: null,
          youtubeUrl: ''
      };
      this.showEditor = true;
  }

  openEditMod(mod: Mod) {
      this.isEditing = true;
      this.editingMod = { ...mod }; // Clonar para evitar cambios en tiempo real sin guardar
      this.showEditor = true;
  }

  closeEditor() {
      this.showEditor = false;
      this.editingMod = null;
  }

  saveMod(mod: Mod) {
      if (this.isEditing) {
          this.modService.updateMod(mod.id, mod).subscribe({
              next: () => {
                  this.loadCatalog();
                  this.closeEditor();
              },
              error: (err) => alert(err.error || 'Error al actualizar el mod')
          });
      } else {
          this.modService.createMod(mod).subscribe({
              next: () => {
                  this.loadCatalog();
                  this.closeEditor();
              },
              error: (err) => alert(err.error || 'Error al crear el mod')
          });
      }
  }

  deleteMod(id: number) {
      if(confirm('¿Estás seguro de que deseas eliminar este Mod permanentemente?')) {
          this.modService.deleteMod(id).subscribe({
              next: () => this.loadCatalog(),
              error: (err) => alert('Error al eliminar el mod')
          });
      }
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
