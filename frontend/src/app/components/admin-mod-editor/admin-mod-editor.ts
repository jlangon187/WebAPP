import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Mod } from '../../services/mod/mod.service';

@Component({
  selector: 'app-admin-mod-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-mod-editor.html'
})
export class AdminModEditorComponent {
  @Input() mod: Mod = {
    id: 0,
    nombre: '',
    descripcion: '',
    precio: 0,
    version: '1.0',
    archivoOriginal: ''
  };
  @Input() isEdit: boolean = false;
  
  @Output() save = new EventEmitter<Mod>();
  @Output() cancel = new EventEmitter<void>();

  onSubmit() {
    this.save.emit(this.mod);
  }

  onCancel() {
    this.cancel.emit();
  }
}
