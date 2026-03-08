import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth/auth.service';
import { HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  stats: any = { totalSales: 0, newUsers: 0, activeTickets: 0 };
  loading = true;
  error = '';

  constructor(private http: HttpClient, private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    const token = this.authService.getToken();
    if(!token) {
        this.router.navigate(['/login']);
        return;
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    this.http.get('http://localhost:8080/api/admin/stats', { headers }).subscribe({
        next: (res: any) => {
            this.stats = res;
            this.loading = false;
        },
        error: (err) => {
            this.error = 'No tienes permiso para acceder al panel de administración o la sesión expiró.';
            this.loading = false;
        }
    });
  }
}
