import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminUser, AdminPurchase, ModService } from '../../services/mod/mod.service';

@Component({
  selector: 'app-admin-users-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users-manager.component.html'
})
export class AdminUsersManagerComponent implements OnInit {
  users: AdminUser[] = [];
  loading = true;
  error = '';
  success = '';
  searchTerm = '';
  expandedUserId: number | null = null;

  editingUserId: number | null = null;
  editForm: {
    nombre: string;
    email: string;
    guid: string;
    rol: string;
    password: string;
  } = {
    nombre: '',
    email: '',
    guid: '',
    rol: 'registrado',
    password: ''
  };

  constructor(private modService: ModService, private router: Router) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';
    this.modService.getAdminUsers().subscribe({
      next: (users) => {
        this.users = users || [];
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los usuarios.';
        this.loading = false;
      }
    });
  }

  get filteredUsers(): AdminUser[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.users;
    }

    return this.users.filter((user) =>
      user.nombre?.toLowerCase().includes(term) ||
      user.email?.toLowerCase().includes(term) ||
      (user.guid || '').toLowerCase().includes(term) ||
      (user.rol || '').toLowerCase().includes(term)
    );
  }

  togglePurchases(userId: number): void {
    this.expandedUserId = this.expandedUserId === userId ? null : userId;
  }

  startEdit(user: AdminUser): void {
    this.success = '';
    this.error = '';
    this.editingUserId = user.id;
    this.editForm = {
      nombre: user.nombre || '',
      email: user.email || '',
      guid: user.guid || '',
      rol: user.rol || 'registrado',
      password: ''
    };
  }

  cancelEdit(): void {
    this.editingUserId = null;
    this.editForm.password = '';
  }

  saveUser(user: AdminUser): void {
    if (this.editingUserId !== user.id) {
      return;
    }

    const payload: any = {
      nombre: this.editForm.nombre?.trim(),
      email: this.editForm.email?.trim(),
      guid: this.editForm.guid?.trim().toUpperCase(),
      rol: this.editForm.rol
    };

    if ((this.editForm.password || '').trim()) {
      payload.password = this.editForm.password.trim();
    }

    this.modService.updateAdminUser(user.id, payload).subscribe({
      next: (updated) => {
        this.users = this.users.map((candidate) => candidate.id === user.id ? updated : candidate);
        this.success = `Usuario ${updated.nombre} actualizado correctamente.`;
        this.error = '';
        this.cancelEdit();
      },
      error: (err) => {
        this.success = '';
        this.error = err?.error || 'No se pudo actualizar el usuario.';
      }
    });
  }

  getPurchaseImage(purchase: AdminPurchase): string {
    return purchase.mod?.archivoOriginal?.trim() ? purchase.mod.archivoOriginal : '/logo.png';
  }

  goBack(): void {
    this.router.navigate(['/admin']);
  }
}
