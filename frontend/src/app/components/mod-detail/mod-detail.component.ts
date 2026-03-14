import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ModService, Mod } from '../../services/mod/mod.service';
import { CartService } from '../../services/cart/cart.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-mod-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mod-detail.component.html',
  styleUrls: ['./mod-detail.component.css']
})
export class ModDetailComponent implements OnInit {
  mod: Mod | null = null;
  youtubeEmbedUrl: SafeResourceUrl | null = null;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private modService: ModService,
    private cartService: CartService,
    private router: Router,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.modService.getModDetails(+idParam).subscribe({
        next: (data) => {
          this.mod = data;
          this.youtubeEmbedUrl = this.buildYoutubeEmbedUrl(this.mod.youtubeUrl);
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
}
