import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ModService, Mod, Comentario } from '../../services/mod/mod.service';
import { CartService } from '../../services/cart/cart.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-mod-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mod-detail.component.html',
  styleUrls: ['./mod-detail.component.css']
})
export class ModDetailComponent implements OnInit {
  mod: Mod | null = null;
  comentarios: Comentario[] = [];
  promedioPuntuacion = 0;
  comentarioForm = {
    puntuacion: 5,
    mensaje: ''
  };
  comentarioError = '';
  comentarioSuccess = '';
  sendingComentario = false;
  youtubeEmbedUrl: SafeResourceUrl | null = null;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private modService: ModService,
    private cartService: CartService,
    private router: Router,
    private sanitizer: DomSanitizer,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.modService.getModDetails(+idParam).subscribe({
        next: (data) => {
          this.mod = data;
          this.youtubeEmbedUrl = this.buildYoutubeEmbedUrl(this.mod.youtubeUrl);
          this.loadComentarios(this.mod.id);
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Mod details could not be loaded.';
          this.loading = false;
        }
      });
    } else {
      this.error = 'Invalid Mod ID';
      this.loading = false;
    }
  }

  addToCart() {
    if (this.mod) {
      this.cartService.addToCart(this.mod);
      this.router.navigate(['/cart']);
    }
  }

  goBack() {
    this.router.navigate(['/catalog']);
  }

  submitComentario() {
    this.comentarioError = '';
    this.comentarioSuccess = '';

    if (!this.mod) {
      return;
    }

    const mensaje = this.comentarioForm.mensaje.trim();
    if (!mensaje) {
      this.comentarioError = 'Escribe un comentario antes de enviar.';
      return;
    }

    this.sendingComentario = true;
    this.modService.createComentario(this.mod.id, this.comentarioForm.puntuacion, mensaje).subscribe({
      next: () => {
        this.sendingComentario = false;
        this.comentarioForm.mensaje = '';
        this.comentarioForm.puntuacion = 5;
        this.comentarioSuccess = 'Comentario publicado correctamente.';
        this.loadComentarios(this.mod!.id);
      },
      error: (err) => {
        this.sendingComentario = false;
        this.comentarioError = err?.error || 'No se pudo publicar el comentario.';
      }
    });
  }

  deleteComentario(id: number) {
    this.modService.deleteComentario(id).subscribe({
      next: () => {
        if (this.mod) {
          this.loadComentarios(this.mod.id);
        }
      },
      error: () => {
        this.comentarioError = 'No se pudo eliminar el comentario.';
      }
    });
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isAdmin(): boolean {
    const user = this.authService.getCurrentUser();
    return (user?.rol || '').toLowerCase() === 'admin';
  }

  getStarArray(value: number): number[] {
    return Array.from({ length: Math.max(0, Math.min(5, value)) }, (_, i) => i);
  }

  getModImage(): string {
    if (!this.mod?.archivoOriginal?.trim()) {
      return '/logo.png';
    }
    return this.mod.archivoOriginal;
  }

  private buildYoutubeEmbedUrl(youtubeUrl?: string | null): SafeResourceUrl | null {
    if (!youtubeUrl) {
      return null;
    }

    try {
      const url = new URL(youtubeUrl);
      let videoId = '';

      if (url.hostname.includes('youtu.be')) {
        videoId = url.pathname.replace('/', '');
      }

      if (url.hostname.includes('youtube.com')) {
        videoId = url.searchParams.get('v') || '';
      }

      if (!videoId) {
        return null;
      }

      const embedUrl = `https://www.youtube.com/embed/${videoId}`;
      return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
    } catch {
      return null;
    }
  }

  private loadComentarios(modId: number) {
    this.modService.getComentarios(modId).subscribe({
      next: (data) => {
        this.comentarios = data || [];
        if (!this.comentarios.length) {
          this.promedioPuntuacion = 0;
          return;
        }
        const total = this.comentarios.reduce((acc, c) => acc + (c.puntuacion || 0), 0);
        this.promedioPuntuacion = total / this.comentarios.length;
      },
      error: () => {
        this.comentarios = [];
        this.promedioPuntuacion = 0;
      }
    });
  }
}
