import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Mod } from '../mod/mod.service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartItems = new BehaviorSubject<Mod[]>([]);
  public cartItems$ = this.cartItems.asObservable();

  constructor() {
    const saved = localStorage.getItem('cart');
    if (saved) {
      this.cartItems.next(JSON.parse(saved));
    }
  }

  addToCart(mod: Mod) {
    const current = this.cartItems.getValue();
    if (!current.find(item => item.id === mod.id)) {
      const updated = [...current, mod];
      this.cartItems.next(updated);
      localStorage.setItem('cart', JSON.stringify(updated));
    }
  }

  removeFromCart(modId: number) {
    const current = this.cartItems.getValue();
    const updated = current.filter(item => item.id !== modId);
    this.cartItems.next(updated);
    localStorage.setItem('cart', JSON.stringify(updated));
  }

  clearCart() {
    this.cartItems.next([]);
    localStorage.removeItem('cart');
  }

  getTotal(): number {
    return this.cartItems.getValue().reduce((total, item) => total + item.precio, 0);
  }

  getItems(): Mod[] {
      return this.cartItems.getValue();
  }
}
