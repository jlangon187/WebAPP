import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../services/cart/cart.service';
import { ModService, Mod } from '../../services/mod/mod.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  cartItems: Mod[] = [];
  total = 0;
  metodoPago = 'Simulacion';
  isProcessing = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private cartService: CartService,
    private modService: ModService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cartItems = this.cartService.getItems();
    this.total = this.cartService.getTotal();

    if (this.cartItems.length === 0) {
      this.router.navigate(['/catalog']);
    }
  }

  processPayment() {
    if (this.cartItems.length === 0) {
      this.router.navigate(['/catalog']);
      return;
    }

    this.isProcessing = true;
    this.errorMessage = '';

    const requests = this.cartItems.map((item) => this.modService.purchaseMod(item.id, this.metodoPago));

    forkJoin(requests).subscribe({
      next: () => {
        this.successMessage = `Compra completada. Se han procesado ${this.cartItems.length} mods correctamente.`;
        this.cartService.clearCart();
        this.isProcessing = false;
        setTimeout(() => this.router.navigate(['/dashboard']), 3000);
      },
      error: (err) => {
        this.errorMessage = err.error || 'Payment failed';
        this.isProcessing = false;
      }
    });
  }
}
