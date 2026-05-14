import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Mod, ModService } from '../../services/mod/mod.service';
import { AdminModEditorComponent } from '../admin-mod-editor/admin-mod-editor';

@Component({
  selector: 'app-admin-mods-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminModEditorComponent],
  templateUrl: './admin-mods-manager.component.html'
})
export class AdminModsManagerComponent implements OnInit {
  mods: Mod[] = [];
  loading = true;
  error = '';
  searchTerm = '';
  categoryFilter = 'all';
  featuredFilter = 'all';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  showEditor = false;
  isEditing = false;
  editingMod: Mod | null = null;

  constructor(private modService: ModService, private router: Router) {}

  ngOnInit(): void {
    this.loadCatalog();
  }

  loadCatalog() {
    this.modService.getCatalog().subscribe({
      next: (mods) => {
        this.mods = mods;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el catálogo de mods.';
        this.loading = false;
      }
    });
  }

  get filteredMods(): Mod[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.mods.filter((mod) => {
      const matchSearch = !term ||
        (mod.nombre || '').toLowerCase().includes(term) ||
        (mod.version || '').toLowerCase().includes(term) ||
        (mod.categoria?.nombre || '').toLowerCase().includes(term);

      const matchCategory = this.categoryFilter === 'all' || (mod.categoria?.nombre || '') === this.categoryFilter;
      const matchFeatured = this.featuredFilter === 'all' ||
        (this.featuredFilter === 'featured' && !!mod.destacadoHome) ||
        (this.featuredFilter === 'not-featured' && !mod.destacadoHome);

      const price = Number(mod.precio || 0);
      const matchMinPrice = this.minPrice == null || Number.isNaN(this.minPrice) || price >= this.minPrice;
      const matchMaxPrice = this.maxPrice == null || Number.isNaN(this.maxPrice) || price <= this.maxPrice;

      return matchSearch && matchCategory && matchFeatured && matchMinPrice && matchMaxPrice;
    });
  }

  get categories(): string[] {
    return Array.from(new Set(this.mods.map((mod) => mod.categoria?.nombre).filter((name): name is string => !!name))).sort((a, b) => a.localeCompare(b));
  }

  openCreateMod() {
    this.isEditing = false;
    this.editingMod = {
      id: 0,
      nombre: '',
      descripcion: '',
      precio: 0,
      version: '1.0',
      archivoOriginal: '',
      categoria: null,
      destacadoHome: false,
      ordenShowroom: null,
      youtubeUrl: ''
    };
    this.showEditor = true;
  }

  openEditMod(mod: Mod) {
    this.isEditing = true;
    this.editingMod = { ...mod };
    this.showEditor = true;
  }

  closeEditor() {
    this.showEditor = false;
    this.editingMod = null;
  }

  saveMod(mod: Mod) {
    if (this.isEditing) {
      this.modService.updateMod(mod.id, mod).subscribe({
        next: () => {
          this.loadCatalog();
          this.closeEditor();
        },
        error: (err) => alert(err.error || 'Error al actualizar el mod')
      });
    } else {
      this.modService.createMod(mod).subscribe({
        next: () => {
          this.loadCatalog();
          this.closeEditor();
        },
        error: (err) => alert(err.error || 'Error al crear el mod')
      });
    }
  }

  deleteMod(id: number) {
    if (confirm('¿Eliminar este mod permanentemente?')) {
      this.modService.deleteMod(id).subscribe({
        next: () => this.loadCatalog(),
        error: () => alert('Error al eliminar el mod')
      });
    }
  }

  goBack() {
    this.router.navigate(['/admin']);
  }
}
