import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../services/cart/cart.service';
import { ModService, Mod } from '../../services/mod/mod.service';

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
    this.isProcessing = true;
    this.errorMessage = '';
    
    // Attempting to buy the first item for now (expandable to cart array)
    // To properly support cart we would loop through items or adjust backend to accept list
    const modToBuy = this.cartItems[0];
    
    if(!modToBuy) return;

    this.modService.purchaseMod(modToBuy.id, this.metodoPago).subscribe({
      next: (res) => {
        this.successMessage = res;
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
