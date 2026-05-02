import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Mod, ModService } from '../../services/mod/mod.service';
import { AdminModEditorComponent } from '../admin-mod-editor/admin-mod-editor';

@Component({
  selector: 'app-admin-mods-manager',
  standalone: true,
  imports: [CommonModule, AdminModEditorComponent],
  templateUrl: './admin-mods-manager.component.html'
})
export class AdminModsManagerComponent implements OnInit {
  mods: Mod[] = [];
  loading = true;
  error = '';
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
