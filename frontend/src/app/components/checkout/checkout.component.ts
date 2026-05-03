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
      this.confirmExternalPayment(provider);
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

  private confirmExternalPayment(providerRaw: string | null) {
    const provider = (providerRaw || '').toLowerCase();
    const isStripe = provider === 'stripe';
    const isPaypal = provider === 'paypal';
    if (!isStripe && !isPaypal) {
      this.errorMessage = 'Proveedor de pago no reconocido en el retorno.';
      return;
    }

    const externalId = isStripe
      ? (this.route.snapshot.queryParamMap.get('session_id') || '').trim()
      : (this.route.snapshot.queryParamMap.get('token') || '').trim();

    if (!externalId) {
      this.errorMessage = 'No se recibió identificador de pago del proveedor.';
      return;
    }

    const modIds = this.cartItems.map(item => item.id);
    if (!modIds.length) {
      this.errorMessage = 'No hay items en carrito para finalizar la compra.';
      return;
    }

    this.isProcessing = true;
    this.modService.confirmPayment(isStripe ? 'stripe' : 'paypal', externalId, modIds).subscribe({
      next: (res) => {
        this.isProcessing = false;
        this.successMessage = res?.message || `Pago ${provider} confirmado.`;
        this.cartService.clearCart();
        setTimeout(() => this.router.navigate(['/dashboard']), 2500);
      },
      error: (err) => {
        this.isProcessing = false;
        this.errorMessage = err?.error || 'No se pudo confirmar el pago con el proveedor.';
      }
    });
  }
}
