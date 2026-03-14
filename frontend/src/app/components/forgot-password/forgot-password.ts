import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './forgot-password.html'
})
export class ForgotPasswordComponent {
  email: string = '';
  loading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(private http: HttpClient, private router: Router) {}

  onSubmit() {
    if (!this.email) return;

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.http.post('/api/auth/forgot-password', { email: this.email }, { responseType: 'text' })
      .subscribe({
        next: (response: any) => {
          this.loading = false;
          // El backend devuelve texto plano en ResponseEntity.ok()
          this.successMessage = response;
        },
        error: (error: HttpErrorResponse) => {
          this.loading = false;
          if (error.status === 400 && error.error) {
            this.errorMessage = error.error;
          } else {
            this.errorMessage = 'Hubo un error al intentar enviar el correo. Inténtalo más tarde.';
          }
        }
      });
  }
}
