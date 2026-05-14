import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ModService, Mod, Categoria, ModRatingSummary, ModPurchaseStats } from '../../services/mod/mod.service';
import { CartService } from '../../services/cart/cart.service';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.css']
})
export class CatalogComponent implements OnInit {
  sortOptions = [
    { value: 'featured', label: 'Destacados' },
    { value: 'top-rated', label: 'Mejor valorados' },
    { value: 'best-selling', label: 'Mas vendidos' },
    { value: 'newest', label: 'Mas recientes' },
    { value: 'price-asc', label: 'Precio ascendente' },
    { value: 'price-desc', label: 'Precio descendente' },
    { value: 'name-asc', label: 'Nombre A-Z' }
  ];
  mods: Mod[] = [];
  filteredMods: Mod[] = [];
  categorias: Categoria[] = [];
  categoriaActiva = 'Todos';
  activeSort = 'featured';
  searchTerm = '';
  ratingsByMod: Record<number, ModRatingSummary> = {};
  purchasesByMod: Record<number, ModPurchaseStats> = {};
  loading = true;
  error = '';

  constructor(
    private modService: ModService,
    private cartService: CartService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.modService.getCategorias().subscribe({
      next: (data) => {
        this.categorias = data;
      }
    });

    this.modService.getCatalog().subscribe({
      next: (data) => {
        this.mods = data;
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load catalog.';
        this.loading = false;
      }
    });

    this.modService.getRatingsSummary().subscribe({
      next: (rows) => {
        this.ratingsByMod = (rows || []).reduce((acc, row) => {
          acc[row.modId] = row;
          return acc;
        }, {} as Record<number, ModRatingSummary>);
        this.applyFilter();
      }
    });

    this.modService.getPurchaseStats().subscribe({
      next: (rows) => {
        this.purchasesByMod = (rows || []).reduce((acc, row) => {
          acc[row.modId] = row;
          return acc;
        }, {} as Record<number, ModPurchaseStats>);
        this.applyFilter();
      }
    });

    this.route.queryParamMap.subscribe(params => {
      const q = (params.get('q') || '').trim().toLowerCase();
      this.searchTerm = q;
      this.applyFilter();
    });
  }

  viewDetails(id: number) {
    this.router.navigate(['/mod', id]);
  }

  setCategoria(categoriaNombre: string) {
    this.categoriaActiva = categoriaNombre;
    this.applyFilter();
  }

  setSort(sort: string) {
    this.activeSort = sort;
    this.applyFilter();
  }

  private applyFilter() {
    const byCategoria = this.categoriaActiva === 'Todos'
      ? this.mods
      : this.mods.filter((mod) => mod.categoria?.nombre === this.categoriaActiva);

    const baseList = !this.searchTerm
      ? [...byCategoria]
      : byCategoria.filter((mod) => {
          const nombre = (mod.nombre || '').toLowerCase();
          const descripcion = (mod.descripcion || '').toLowerCase();
          const categoria = (mod.categoria?.nombre || '').toLowerCase();
          return nombre.includes(this.searchTerm) || descripcion.includes(this.searchTerm) || categoria.includes(this.searchTerm);
        });

    this.filteredMods = baseList.sort((a, b) => this.compareMods(a, b));
  }

  private compareMods(a: Mod, b: Mod): number {
    switch (this.activeSort) {
      case 'top-rated': {
        const ratingA = this.getModRating(a.id);
        const ratingB = this.getModRating(b.id);
        if (ratingB !== ratingA) return ratingB - ratingA;
        const countA = this.getModRatingCount(a.id);
        const countB = this.getModRatingCount(b.id);
        if (countB !== countA) return countB - countA;
        return this.compareByName(a, b);
      }
      case 'best-selling': {
        const soldA = this.getModPurchases(a.id);
        const soldB = this.getModPurchases(b.id);
        if (soldB !== soldA) return soldB - soldA;
        const ratingA = this.getModRating(a.id);
        const ratingB = this.getModRating(b.id);
        if (ratingB !== ratingA) return ratingB - ratingA;
        return this.compareByName(a, b);
      }
      case 'newest': {
        const createdA = this.getCreatedAtTime(a);
        const createdB = this.getCreatedAtTime(b);
        if (createdB !== createdA) return createdB - createdA;
        return this.compareByName(a, b);
      }
      case 'price-asc':
        return (a.precio || 0) - (b.precio || 0) || this.compareByName(a, b);
      case 'price-desc':
        return (b.precio || 0) - (a.precio || 0) || this.compareByName(a, b);
      case 'name-asc':
        return this.compareByName(a, b);
      case 'featured':
      default: {
        const featuredA = !!a.destacadoHome;
        const featuredB = !!b.destacadoHome;
        if (featuredA !== featuredB) return featuredA ? -1 : 1;
        if (featuredA && featuredB) {
          const orderA = a.ordenShowroom ?? Number.MAX_SAFE_INTEGER;
          const orderB = b.ordenShowroom ?? Number.MAX_SAFE_INTEGER;
          if (orderA !== orderB) return orderA - orderB;
        }
        return this.compareByName(a, b);
      }
    }
  }

  private compareByName(a: Mod, b: Mod): number {
    return (a.nombre || '').localeCompare(b.nombre || '', 'es', { sensitivity: 'base' });
  }

  private getCreatedAtTime(mod: Mod): number {
    const value = (mod as any).creadoEn;
    if (!value) return 0;
    const time = new Date(value).getTime();
    return Number.isNaN(time) ? 0 : time;
  }

  getModImage(mod: Mod): string {
    return mod.archivoOriginal?.trim() ? mod.archivoOriginal : '/logo.png';
  }

  addToCart(mod: Mod, event: Event) {
    event.stopPropagation();
    this.cartService.addToCart(mod);
  }

  getModRating(modId: number): number {
    const row = this.ratingsByMod[modId];
    return row ? row.avgPuntuacion : 0;
  }

  getModRatingCount(modId: number): number {
    const row = this.ratingsByMod[modId];
    return row ? row.totalComentarios : 0;
  }

  getModPurchases(modId: number): number {
    const row = this.purchasesByMod[modId];
    return row ? row.totalPurchases : 0;
  }
}
