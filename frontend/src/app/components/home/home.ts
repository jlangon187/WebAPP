import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ModService, Mod, ModRatingSummary, RecentReview } from '../../services/mod/mod.service';
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
  ratingsByMod: Record<number, ModRatingSummary> = {};
  recentReviews: RecentReview[] = [];
  currentReviewSlide = 0;
  reviewInterval: any;
  private touchStartX = 0;
  private touchCurrentX = 0;
  private readonly swipeThreshold = 50;
  private resizeHandler = () => this.onViewportChange();

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

    this.modService.getRatingsSummary().subscribe({
      next: (rows) => {
        this.ratingsByMod = (rows || []).reduce((acc, row) => {
          acc[row.modId] = row;
          return acc;
        }, {} as Record<number, ModRatingSummary>);
      }
    });

    this.modService.getRecentReviews().subscribe({
      next: (reviews) => {
        this.recentReviews = reviews || [];
        this.onViewportChange();
      },
      error: () => {
        this.recentReviews = [];
      }
    });

    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.resizeHandler);
      window.addEventListener('orientationchange', this.resizeHandler);
    }
  }

  ngOnDestroy(): void {
    if (this.slideInterval) {
      clearInterval(this.slideInterval);
    }
    if (this.reviewInterval) {
      clearInterval(this.reviewInterval);
    }
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.resizeHandler);
      window.removeEventListener('orientationchange', this.resizeHandler);
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

  startReviewCarousel() {
    if (this.reviewInterval) {
      clearInterval(this.reviewInterval);
    }
    this.reviewInterval = setInterval(() => this.nextReviewSlide(), 6000);
  }

  nextReviewSlide() {
    const totalPages = this.reviewPageCount();
    if (totalPages <= 1) {
      return;
    }
    this.currentReviewSlide = (this.currentReviewSlide + 1) % totalPages;
  }

  prevReviewSlide() {
    const totalPages = this.reviewPageCount();
    if (totalPages <= 1) {
      return;
    }
    this.currentReviewSlide = (this.currentReviewSlide - 1 + totalPages) % totalPages;
  }

  setReviewSlide(index: number) {
    this.currentReviewSlide = index;
    this.startReviewCarousel();
  }

  onReviewTouchStart(event: TouchEvent) {
    if (!event.touches.length) return;
    this.touchStartX = event.touches[0].clientX;
    this.touchCurrentX = this.touchStartX;
    if (this.reviewInterval) {
      clearInterval(this.reviewInterval);
      this.reviewInterval = null;
    }
  }

  onReviewTouchMove(event: TouchEvent) {
    if (!event.touches.length) return;
    this.touchCurrentX = event.touches[0].clientX;
  }

  onReviewTouchEnd() {
    const delta = this.touchCurrentX - this.touchStartX;
    if (Math.abs(delta) >= this.swipeThreshold) {
      if (delta < 0) {
        this.nextReviewSlide();
      } else {
        this.prevReviewSlide();
      }
    }
    this.startReviewCarousel();
  }

  visibleReviews(): RecentReview[] {
    const perPage = this.reviewVisibleCount();
    const start = this.currentReviewSlide * perPage;
    return this.recentReviews.slice(start, start + perPage);
  }

  reviewPageCount(): number {
    const perPage = this.reviewVisibleCount();
    if (perPage <= 0) {
      return 1;
    }
    return Math.max(1, Math.ceil(this.recentReviews.length / perPage));
  }

  reviewPages(): number[] {
    return Array.from({ length: this.reviewPageCount() }, (_, i) => i);
  }

  reviewVisibleCount(): number {
    if (typeof window === 'undefined') {
      return 3;
    }
    const width = window.innerWidth;
    if (width < 768) return 1;
    if (width < 1280) return 2;
    return 3;
  }

  private onViewportChange() {
    const totalPages = this.reviewPageCount();
    if (this.currentReviewSlide >= totalPages) {
      this.currentReviewSlide = Math.max(0, totalPages - 1);
    }
    if (this.recentReviews.length > this.reviewVisibleCount()) {
      this.startReviewCarousel();
    } else if (this.reviewInterval) {
      clearInterval(this.reviewInterval);
      this.reviewInterval = null;
    }
  }

  reviewStars(value: number): number[] {
    const safe = Math.max(1, Math.min(5, value || 0));
    return Array.from({ length: safe }, (_, i) => i);
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

  getModRating(modId: number): number {
    const row = this.ratingsByMod[modId];
    return row ? row.avgPuntuacion : 0;
  }

  getModRatingCount(modId: number): number {
    const row = this.ratingsByMod[modId];
    return row ? row.totalComentarios : 0;
  }
}
