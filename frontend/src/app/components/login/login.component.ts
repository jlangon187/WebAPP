import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  credentials = { email: '', password: '' };
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router, private route: ActivatedRoute) {
    const oauthError = this.route.snapshot.queryParamMap.get('error') || '';
    if (oauthError) {
      this.error = this.mapOauthError(oauthError);
    }
  }

  private mapOauthError(errorCode: string): string {
    switch (errorCode) {
      case 'discord-cancelled':
        return 'Has cancelado el inicio de sesion con Discord.';
      case 'discord-missing-code':
        return 'No se pudo completar el inicio de sesion con Discord. Intentalo de nuevo.';
      case 'account-disabled':
        return 'Tu cuenta esta desactivada. Contacta con el administrador.';
      case 'discord':
        return 'Error al iniciar sesion con Discord. Intentalo de nuevo en unos minutos.';
      default:
        return 'No se pudo completar el inicio de sesion. Intentalo de nuevo.';
    }
  }

  onSubmit() {
    this.loading = true;
    this.error = '';

    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.router.navigate(['/catalog']);
      },
      error: (err) => {
        if(err.status === 401) {
            this.error = 'Correo o contrasena incorrectos.';
        } else {
            this.error = 'Ha ocurrido un error. Intentalo de nuevo mas tarde.';
        }
        this.loading = false;
      }
    });
  }
}
