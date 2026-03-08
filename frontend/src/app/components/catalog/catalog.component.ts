import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ModService, Mod } from '../../services/mod/mod.service';
import { CartService } from '../../services/cart/cart.service';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.css']
})
export class CatalogComponent implements OnInit {
  mods: Mod[] = [];
  loading = true;
  error = '';

  constructor(
    private modService: ModService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.modService.getCatalog().subscribe({
      next: (data) => {
        this.mods = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load catalog.';
        this.loading = false;
      }
    });
  }

  viewDetails(id: number) {
    this.router.navigate(['/mod', id]);
  }

  addToCart(mod: Mod, event: Event) {
    event.stopPropagation();
    this.cartService.addToCart(mod);
  }
}
