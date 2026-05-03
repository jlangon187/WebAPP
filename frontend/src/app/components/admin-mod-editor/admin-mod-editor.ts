import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Mod } from '../../services/mod/mod.service';

@Component({
  selector: 'app-admin-mod-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-mod-editor.html'
})
export class AdminModEditorComponent implements OnChanges {
  @Input() mod: Mod = {
    id: 0,
    nombre: '',
    descripcion: '',
    precio: 0,
    version: '1.0',
    archivoOriginal: '',
    categoria: null,
    destacadoHome: false,
    ordenShowroom: null,
    youtubeUrl: '',
    carpetaBaseMod: ''
  };
  @Input() isEdit: boolean = false;
  
  @Output() save = new EventEmitter<Mod>();
  @Output() cancel = new EventEmitter<void>();

  readonly categoriasFijas: string[] = ['Motos', 'Bikesets', 'Liveries', 'Sonidos', 'Circuitos', 'Equipamiento', 'UI'];
  validationError = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['mod'] && this.mod) {
      if (typeof this.mod.destacadoHome !== 'boolean') {
        this.mod.destacadoHome = false;
      }
      if (!this.mod.destacadoHome) {
        this.mod.ordenShowroom = null;
      }
      if (!this.mod.youtubeUrl) {
        this.mod.youtubeUrl = '';
      }
      if (!this.mod.carpetaBaseMod) {
        this.mod.carpetaBaseMod = '';
      }
      if (this.mod.categoria && !this.mod.categoria.nombre) {
        this.mod.categoria = null;
      }
    }
  }

  onFeaturedChange() {
    if (!this.mod.destacadoHome) {
      this.mod.ordenShowroom = null;
    }
  }

  onCategoriaChange(nombreCategoria: string) {
    if (!nombreCategoria) {
      this.mod.categoria = null;
      return;
    }

    if (this.mod.categoria?.nombre === nombreCategoria) {
      return;
    }

    this.mod.categoria = { nombre: nombreCategoria } as any;
  }

  onFilePicked(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.mod.archivoOriginal = `/home/${file.name}`;
  }

  getCategoriaActual(): string {
    return this.mod.categoria?.nombre || '';
  }

  onSubmit() {
    this.validationError = '';

    if (this.mod.destacadoHome && (!this.mod.ordenShowroom || this.mod.ordenShowroom < 1 || this.mod.ordenShowroom > 3)) {
      this.validationError = 'Debes seleccionar una posicion de showroom valida (1, 2 o 3).';
      return;
    }

    if (!this.mod.destacadoHome) {
      this.mod.ordenShowroom = null;
    }

    this.save.emit(this.mod);
  }

  onCancel() {
    this.cancel.emit();
  }
}
