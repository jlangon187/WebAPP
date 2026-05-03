import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
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
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.cartItems = this.cartService.getItems();
    this.total = this.cartService.getTotal();

    const paymentStatus = this.route.snapshot.queryParamMap.get('payment');
    const provider = this.route.snapshot.queryParamMap.get('provider');
    if (paymentStatus === 'success') {
      this.successMessage = `Pago ${provider || ''} autorizado. Finalizacion automatica pendiente (webhook).`;
    }
    if (paymentStatus === 'cancel') {
      this.errorMessage = `Pago ${provider || ''} cancelado por el usuario.`;
    }

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

    if (this.metodoPago === 'Simulacion') {
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
      return;
    }

    const provider = this.metodoPago === 'Stripe' ? 'stripe' : 'paypal';
    const modIds = this.cartItems.map(item => item.id);
    this.modService.createPaymentSession(provider as 'stripe' | 'paypal', modIds).subscribe({
      next: (session) => {
        this.isProcessing = false;
        if (!session?.redirectUrl) {
          this.errorMessage = 'No se recibió URL de pago del proveedor.';
          return;
        }
        window.location.href = session.redirectUrl;
      },
      error: (err) => {
        this.isProcessing = false;
        this.errorMessage = err?.error || 'No se pudo iniciar la sesion de pago.';
      }
    });
  }
}
