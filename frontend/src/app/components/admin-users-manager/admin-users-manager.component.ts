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
  roleFilter = 'all';
  statusFilter = 'all';
  profileFilter = 'all';
  purchasesFilter = 'all';
  ticketsFilter = 'all';
  expandedUserId: number | null = null;
  editingPurchaseGuidId: number | null = null;
  purchaseGuidDraft = '';

  editingUserId: number | null = null;
  editForm: {
    nombre: string;
    email: string;
    guid: string;
    rol: string;
    activo: boolean;
    password: string;
  } = {
    nombre: '',
    email: '',
    guid: '',
    rol: 'registrado',
    activo: true,
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
    return this.users.filter((user) => {
      const matchSearch = !term ||
        user.nombre?.toLowerCase().includes(term) ||
        user.email?.toLowerCase().includes(term) ||
        (user.guid || '').toLowerCase().includes(term) ||
        (user.rol || '').toLowerCase().includes(term);

      const matchRole = this.roleFilter === 'all' || (user.rol || '').toLowerCase() === this.roleFilter;
      const matchStatus = this.statusFilter === 'all' ||
        (this.statusFilter === 'active' && user.activo) ||
        (this.statusFilter === 'inactive' && !user.activo);
      const matchProfile = this.profileFilter === 'all' ||
        (this.profileFilter === 'complete' && !!user.profileCompleted) ||
        (this.profileFilter === 'incomplete' && !user.profileCompleted);
      const matchPurchases = this.purchasesFilter === 'all' ||
        (this.purchasesFilter === 'with' && (user.purchasesCount || 0) > 0) ||
        (this.purchasesFilter === 'without' && (user.purchasesCount || 0) === 0);
      const matchTickets = this.ticketsFilter === 'all' ||
        (this.ticketsFilter === 'with' && (user.ticketsCount || 0) > 0) ||
        (this.ticketsFilter === 'without' && (user.ticketsCount || 0) === 0);

      return matchSearch && matchRole && matchStatus && matchProfile && matchPurchases && matchTickets;
    });
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
      activo: user.activo !== false,
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
      rol: this.editForm.rol,
      activo: this.editForm.activo
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

  startPurchaseGuidEdit(purchase: AdminPurchase): void {
    this.editingPurchaseGuidId = purchase.id;
    this.purchaseGuidDraft = (purchase.guidCompra || '').toUpperCase();
    this.success = '';
    this.error = '';
  }

  cancelPurchaseGuidEdit(): void {
    this.editingPurchaseGuidId = null;
    this.purchaseGuidDraft = '';
  }

  savePurchaseGuid(user: AdminUser, purchase: AdminPurchase): void {
    const guid = (this.purchaseGuidDraft || '').trim().toUpperCase();
    if (!guid.match(/^[A-F0-9]{18}$/)) {
      this.error = 'El GUID de compra debe tener 18 caracteres hexadecimales.';
      return;
    }

    this.modService.updateAdminPurchaseGuid(user.id, purchase.id, guid).subscribe({
      next: (updatedUser) => {
        this.users = this.users.map((candidate) => candidate.id === user.id ? updatedUser : candidate);
        this.success = `GUID de compra actualizado para ${updatedUser.nombre}.`;
        this.error = '';
        this.cancelPurchaseGuidEdit();
      },
      error: (err) => {
        this.success = '';
        this.error = err?.error || 'No se pudo actualizar el GUID de compra.';
      }
    });
  }

  resendPurchaseEmail(user: AdminUser, purchase: AdminPurchase): void {
    this.success = '';
    this.error = '';
    this.modService.resendAdminPurchaseDownloadEmail(user.id, purchase.id).subscribe({
      next: (message: any) => {
        this.success = typeof message === 'string' ? message : `Correo reenviado para ${user.nombre}.`;
      },
      error: (err) => {
        this.error = err?.error || 'No se pudo reenviar el correo de descarga.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin']);
  }
}
