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
    // Initial static mock data to guarantee beautiful UI immediately
    this.featuredMods = [
      {
        id: 101,
        nombre: 'Kawasaki Ninja ZX-10RR 2025',
        descripcion: 'Experimenta el pico del rendimiento en pista. Físicas rediseñadas, telemetría real y un nivel de detalle en texturas 4K nunca antes visto.',
        precio: 14.99,
        version: '1.2.0',
        archivoOriginal: '/home/kawasaki.png'
      },
      {
        id: 102,
        nombre: 'Yamaha YZF-R1M Track Edition',
        descripcion: 'La bestia de Iwata con escapes Akrapovič completos y suspensiones Öhlins totalmente funcionales en su telemetría gráfica.',
        precio: 12.99,
        version: '1.0.5',
        archivoOriginal: '/home/yamaha.png'
      },
      {
        id: 103,
        nombre: 'Circuito de Jerez-Ángel Nieto Láser Scan',
        descripcion: 'Escaneado por láser con precisión milimétrica. Siente cada bache de la mítica pista española.',
        precio: 9.99,
        version: '2.0.0',
        archivoOriginal: '/home/jerez.jpg'
      }
    ];

    this.startCarousel();

    this.modService.getCatalog().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.trendingMods = data.slice(0, 4);
        } else {
          // Fallback sample data if DB is empty
          this.trendingMods = [
            { id: 201, nombre: 'Ducati Superleggera V4', descripcion: '', precio: 19.99, version: '1.0', archivoOriginal: '/home/ducati.png' },
            { id: 202, nombre: 'Casco Arai RX-7V Racing', descripcion: '', precio: 4.99, version: '2.1', archivoOriginal: '/home/arai.png' },
            { id: 203, nombre: 'Honda CBR1000RR-R SP', descripcion: '', precio: 11.99, version: '1.4', archivoOriginal: '/home/honda.png' },
            { id: 204, nombre: 'Classic 500cc 2-Stroke', descripcion: '', precio: 15.99, version: '1.0', archivoOriginal: '/home/classic.png' },
          ];
        }
        this.loading = false;
      },
      error: (err) => {
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
    this.slideInterval = setInterval(() => {
      this.nextSlide();
    }, 5000); // Rotates every 5 seconds
  }

  nextSlide() {
    this.currentSlide = (this.currentSlide + 1) % this.featuredMods.length;
  }

  setSlide(index: number) {
    this.currentSlide = index;
    clearInterval(this.slideInterval);
    this.startCarousel(); // Restart interval on manual click
  }

  viewDetails(id: number) {
    this.router.navigate(['/mod', id]);
  }

  addToCart(mod: Mod, event: Event) {
    event.stopPropagation();
    this.cartService.addToCart(mod);
  }
}
