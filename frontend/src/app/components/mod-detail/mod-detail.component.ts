import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ModService, Mod } from '../../services/mod/mod.service';
import { CartService } from '../../services/cart/cart.service';

@Component({
  selector: 'app-mod-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mod-detail.component.html',
  styleUrls: ['./mod-detail.component.css']
})
export class ModDetailComponent implements OnInit {
  mod: Mod | null = null;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private modService: ModService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.modService.getModDetails(+idParam).subscribe({
        next: (data) => {
          this.mod = data;
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
}
