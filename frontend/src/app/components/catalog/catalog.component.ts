import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ModService, Mod, Categoria, ModRatingSummary } from '../../services/mod/mod.service';
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
  filteredMods: Mod[] = [];
  categorias: Categoria[] = [];
  categoriaActiva = 'Todos';
  searchTerm = '';
  ratingsByMod: Record<number, ModRatingSummary> = {};
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

  private applyFilter() {
    const byCategoria = this.categoriaActiva === 'Todos'
      ? this.mods
      : this.mods.filter((mod) => mod.categoria?.nombre === this.categoriaActiva);

    if (!this.searchTerm) {
      this.filteredMods = byCategoria;
      return;
    }

    this.filteredMods = byCategoria.filter((mod) => {
      const nombre = (mod.nombre || '').toLowerCase();
      const descripcion = (mod.descripcion || '').toLowerCase();
      const categoria = (mod.categoria?.nombre || '').toLowerCase();
      return nombre.includes(this.searchTerm) || descripcion.includes(this.searchTerm) || categoria.includes(this.searchTerm);
    });
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
}
