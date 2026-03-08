import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  template: '<div class="flex items-center justify-center min-h-screen"><div class="text-center p-10 text-white font-bold text-xl animate-pulse">Authenticating with Discord...</div></div>'
})
export class AuthCallbackComponent implements OnInit {
  constructor(private route: ActivatedRoute, private router: Router, private authService: AuthService) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['token']) {
        const user = {
          token: params['token'],
          rol: params['rol'] || 'registrado',
          guid: params['guid'] || '',
          nombre: params['nombre'] || 'User'
        };
        this.authService.setExternalLogin(user);
        this.router.navigate(['/catalog']);
      } else {
        this.router.navigate(['/login']);
      }
    });
  }
}
