import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reset-password.html'
})
export class ResetPasswordComponent implements OnInit {
  token: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  
  loading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(
    private http: HttpClient, 
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Extraer el token de la URL: /reset-password?token=xxxx-xxxx
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      if (!this.token) {
        this.errorMessage = 'Enlace inválido. Falta el token de seguridad.';
      }
    });
  }

  onSubmit() {
    if (!this.token) {
        this.errorMessage = 'No se ha detectado un token de recuperación válido.';
        return;
    }
    
    if (!this.newPassword || !this.confirmPassword) return;

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden. Por favor, revísalas.';
      return;
    }

    if (this.newPassword.length < 6) {
        this.errorMessage = 'La contraseña debe tener al menos 6 caracteres.';
        return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.http.post('http://localhost:8080/api/auth/reset-password', { 
        token: this.token,
        newPassword: this.newPassword 
    }, { responseType: 'text' })
      .subscribe({
        next: (response: any) => {
          this.loading = false;
          // El backend devuelve texto plano en ResponseEntity.ok()
          this.successMessage = response;
          // Redirigir automáticamente al login tras unos segundos
          setTimeout(() => {
              this.router.navigate(['/login']);
          }, 3000);
        },
        error: (error: HttpErrorResponse) => {
          this.loading = false;
          if (error.status === 400 && error.error) {
            this.errorMessage = error.error;
          } else {
            this.errorMessage = 'El enlace ha caducado o es inválido.';
          }
        }
      });
  }
}
