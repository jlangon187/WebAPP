import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ModService, Mod } from '../../services/mod/mod.service';
import { CartService } from '../../services/cart/cart.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit, OnDestroy {
  trendingMods: Mod[] = [];
  featuredMods: Mod[] = [];
  currentSlide = 0;
  slideInterval: any;
  loading = false;
  error = '';

  constructor(
    private modService: ModService,
    private cartService: CartService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loading = true;

    this.modService.getShowroomMods().subscribe({
      next: (mods) => {
        this.featuredMods = mods;
        if (this.featuredMods.length > 0) {
          this.startCarousel();
        }
      },
      error: () => {
        this.featuredMods = [];
      }
    });

    this.modService.getCatalog().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.trendingMods = data.slice(0, 4);
        } else {
          this.trendingMods = [];
        }
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load trending mods.';
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.slideInterval) {
      clearInterval(this.slideInterval);
    }
  }

  startCarousel() {
    if (!this.featuredMods.length) {
      return;
    }

    if (this.slideInterval) {
      clearInterval(this.slideInterval);
    }

    this.slideInterval = setInterval(() => {
      this.nextSlide();
    }, 5000); // Rotates every 5 seconds
  }

  nextSlide() {
    if (!this.featuredMods.length) {
      return;
    }
    this.currentSlide = (this.currentSlide + 1) % this.featuredMods.length;
  }

  setSlide(index: number) {
    if (!this.featuredMods.length) {
      return;
    }
    this.currentSlide = index;
    if (this.slideInterval) {
      clearInterval(this.slideInterval);
    }
    this.startCarousel(); // Restart interval on manual click
  }

  getModImage(mod: Mod): string {
    return mod.archivoOriginal?.trim() ? mod.archivoOriginal : '/logo.png';
  }

  viewDetails(id: number) {
    this.router.navigate(['/mod', id]);
  }

  addToCart(mod: Mod, event: Event) {
    event.stopPropagation();
    this.cartService.addToCart(mod);
  }
}
