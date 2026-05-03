import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ModService } from '../../services/mod/mod.service';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.css']
})
export class UserDashboardComponent implements OnInit {
  purchases: any[] = [];
  tickets: any[] = [];
  ratings: any[] = [];
  loading = true;
  error = '';
  userName = '';
  isAdmin = false;
  
  isEditing = false;
  editData = { nombre: '', password: '', email: '', guid: '' };
  updateMessage = '';
  updateError = '';
  originalGuid = '';
  preparingDownloads: { [modId: number]: boolean } = {};

  constructor(
    private modService: ModService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser.subscribe(u => {
      if(u) {
        this.userName = u.nombre;
        this.isAdmin = u.rol === 'admin' || u.rol === 'ADMIN';
        this.editData.nombre = u.nombre;
        this.editData.email = u.sub || u.email || '';
        this.editData.guid = u.guid || '';
        this.originalGuid = (u.guid || '').toUpperCase();

        if (!this.isAdmin) {
          this.loadPurchases();
          this.loadTickets();
          this.loadRatings();
        } else {
          this.loading = false;
        }
      }
    });
  }

  loadPurchases() {
    this.modService.getMyPurchases().subscribe({
      next: (data) => {
        this.purchases = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load your purchases.';
        this.loading = false;
      }
    });
  }

  loadTickets() {
    this.modService.getMyTickets().subscribe({
      next: (data) => {
        this.tickets = data || [];
      },
      error: () => {
        this.tickets = [];
      }
    });
  }

  loadRatings() {
    this.modService.getMyComentarios().subscribe({
      next: (data) => {
        this.ratings = data || [];
      },
      error: () => {
        this.ratings = [];
      }
    });
  }

  downloadMod(modId: number) {
    if (this.preparingDownloads[modId]) {
      return;
    }

    this.preparingDownloads[modId] = true;
    this.modService.prepareDownload(modId).subscribe({
      next: (res) => {
        if (res.status === 'DONE' && res.downloadToken) {
          this.preparingDownloads[modId] = false;
          this.downloadPreparedFile(res.downloadToken);
          return;
        }

        this.pollDownloadJob(modId, res.jobId, 0);
      },
      error: (err) => {
        this.preparingDownloads[modId] = false;
        alert(err?.error || 'No se pudo preparar la descarga personalizada.');
      }
    });
  }

  private pollDownloadJob(modId: number, jobId: number, attempt: number) {
    if (attempt > 120) {
      this.preparingDownloads[modId] = false;
      alert('La preparacion esta tardando demasiado. Vuelve a intentarlo en unos minutos.');
      return;
    }

    setTimeout(() => {
      this.modService.getDownloadJobStatus(jobId).subscribe({
        next: (res) => {
          if (res.status === 'DONE' && res.downloadToken) {
            this.preparingDownloads[modId] = false;
            this.downloadPreparedFile(res.downloadToken);
            return;
          }

          if (res.status === 'FAILED') {
            this.preparingDownloads[modId] = false;
            alert(res.message || 'La preparacion ha fallado. Contacta con soporte.');
            return;
          }

          this.pollDownloadJob(modId, jobId, attempt + 1);
        },
        error: () => {
          this.preparingDownloads[modId] = false;
          alert('No se pudo consultar el estado de la descarga.');
        }
      });
    }, 3000);
  }

  private downloadPreparedFile(downloadToken: string) {
    this.modService.downloadPreparedFile(downloadToken).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          alert('No se pudo descargar el archivo generado.');
          return;
        }

        const disposition = response.headers.get('content-disposition') || '';
        const fileName = this.extractFileNameFromDisposition(disposition) || `MOD_${downloadToken}.rar`;
        const blobUrl = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = blobUrl;
        anchor.download = fileName;
        anchor.click();
        window.URL.revokeObjectURL(blobUrl);
      },
      error: () => {
        alert('No se pudo descargar el archivo generado.');
      }
    });
  }

  private extractFileNameFromDisposition(disposition: string): string | null {
    if (!disposition) {
      return null;
    }

    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
      try {
        return decodeURIComponent(utf8Match[1].trim());
      } catch {
        return utf8Match[1].trim();
      }
    }

    const quotedMatch = disposition.match(/filename="([^"]+)"/i);
    if (quotedMatch?.[1]) {
      return quotedMatch[1].trim();
    }

    const plainMatch = disposition.match(/filename=([^;]+)/i);
    if (plainMatch?.[1]) {
      return plainMatch[1].trim();
    }

    return null;
  }

  goToCatalog() {
    this.router.navigate(['/catalog']);
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    this.updateMessage = '';
    this.updateError = '';
  }

  saveProfile() {
    this.updateMessage = '';
    this.updateError = '';

    if (this.isGuidChangingWithPurchases()) {
      const confirmed = window.confirm(
        'ATENCION: Ya tienes compras realizadas. Si cambias tu GUID, los mods comprados seguiran vinculados al GUID original usado en cada compra. Si cambias de GUID en el juego, deberas contactar con el administrador.\n\n¿Deseas continuar?'
      );
      if (!confirmed) {
        return;
      }
    }

    this.authService.updateProfile(this.editData).subscribe({
      next: (res) => {
        if (this.isGuidChangingWithPurchases()) {
          this.updateMessage = 'Perfil actualizado. Aviso: tus compras anteriores siguen vinculadas al GUID original de cada compra. Si cambias de GUID en el juego, contacta con el administrador.';
        } else {
          this.updateMessage = 'Perfil actualizado con éxito. Si cambiaste la contraseña, usa la nueva en el próximo inicio de sesión.';
        }
        
        // The API now returns an AuthResponse with the updated token, rol, guid, and nombre.
        if (res && res.token) {
            this.authService.setExternalLogin(res);
            this.userName = res.nombre;
            // Also update editData so next time we open edit it's correct
            this.editData.nombre = res.nombre;
            this.editData.guid = res.guid;
            this.originalGuid = (res.guid || '').toUpperCase();
            // The new token uses the new email as subject
        }

        setTimeout(() => this.isEditing = false, 3000);
      },
      error: (err) => {
        this.updateError = err.error || 'Error al actualizar el perfil. Inténtalo de nuevo.';
      }
    });
  }

  getPurchaseImage(path?: string) {
    return path?.trim() ? path : '/logo.png';
  }

  isGuidChangingWithPurchases(): boolean {
    const current = (this.editData.guid || '').trim().toUpperCase();
    return this.purchases.length > 0 && !!this.originalGuid && current !== this.originalGuid;
  }

  getTicketSummary(message?: string): string {
    const text = (message || '').trim();
    if (!text) {
      return '';
    }
    return text.length > 140 ? `${text.substring(0, 140)}...` : text;
  }
}
