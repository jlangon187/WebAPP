import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth/auth.service';
import { CartService } from './services/cart/cart.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule, CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'frontend';
  cartCount = 0;
  userName = '';
  isAdmin = false;
  searchQuery = '';

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit() {
    this.cartService.cartItems$.subscribe(items => {
      this.cartCount = items.length;
    });

    this.authService.currentUser.subscribe(user => {
      if (user) {
        this.userName = user.nombre;
        this.isAdmin = user.rol === 'admin' || user.rol === 'ADMIN';
      } else {
        this.userName = '';
        this.isAdmin = false;
      }
    });
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  logout() {
    this.authService.logout();
  }

  performSearch() {
    const query = this.searchQuery.trim();
    this.router.navigate(['/catalog'], {
      queryParams: query ? { q: query } : {}
    });
  }
}
