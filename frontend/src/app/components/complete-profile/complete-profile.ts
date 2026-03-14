import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complete-profile.html',
  styleUrl: './complete-profile.css'
})
export class CompleteProfileComponent {
  form = {
    nombre: '',
    guid: ''
  };
  saving = false;
  error = '';

  constructor(private authService: AuthService, private router: Router) {
    const user = this.authService.getCurrentUser();
    this.form.nombre = user?.nombre || '';
    this.form.guid = user?.guid || '';
  }

  submit() {
    this.error = '';

    const guid = this.form.guid.trim().toUpperCase();
    if (!guid.match(/^[A-F0-9]{8}$/)) {
      this.error = 'El GUID debe tener exactamente 8 caracteres hexadecimales (ej. 51C617A2).';
      return;
    }

    const nombre = this.form.nombre.trim();
    if (!nombre) {
      this.error = 'Debes indicar un nombre de usuario.';
      return;
    }

    this.saving = true;
    this.authService.updateProfile({ nombre, guid }).subscribe({
      next: (res: any) => {
        if (res && res.token) {
          this.authService.setExternalLogin(res);
        }
        this.router.navigate(['/catalog']);
      },
      error: (err) => {
        this.error = err?.error || 'No se pudo guardar tu perfil. Intentalo de nuevo.';
        this.saving = false;
      }
    });
  }
}
