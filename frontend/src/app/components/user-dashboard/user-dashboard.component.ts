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
  loading = true;
  error = '';
  userName = '';
  isAdmin = false;
  
  isEditing = false;
  editData = { nombre: '', password: '', email: '', guid: '' };
  updateMessage = '';
  updateError = '';

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

        if (!this.isAdmin) {
          this.loadPurchases();
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

  downloadMod(modId: number) {
    this.modService.getDownloadUrl(modId).subscribe({
      next: (url) => {
        window.open(url, '_blank');
      },
      error: (err) => {
        alert('Failed to get download link. You might need to purchase it first.');
      }
    });
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
    this.authService.updateProfile(this.editData).subscribe({
      next: (res) => {
        this.updateMessage = 'Perfil actualizado con éxito. Si cambiaste la contraseña, usa la nueva en el próximo inicio de sesión.';
        
        // The API now returns an AuthResponse with the updated token, rol, guid, and nombre.
        if (res && res.token) {
            this.authService.setExternalLogin(res);
            this.userName = res.nombre;
            // Also update editData so next time we open edit it's correct
            this.editData.nombre = res.nombre;
            this.editData.guid = res.guid;
            // The new token uses the new email as subject
        }

        setTimeout(() => this.isEditing = false, 3000);
      },
      error: (err) => {
        this.updateError = err.error || 'Error al actualizar el perfil. Inténtalo de nuevo.';
      }
    });
  }
}
