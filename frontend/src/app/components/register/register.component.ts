import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  userData = { nombre: '', email: '', password: '', guid: '' };
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    this.loading = true;
    this.error = '';

    this.authService.register(this.userData).subscribe({
      next: () => {
        // Auto login after register
        this.authService.login({ email: this.userData.email, password: this.userData.password }).subscribe(() => {
           this.router.navigate(['/catalog']);
        });
      },
      error: (err) => {
        this.error = err.error || 'Registration failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
