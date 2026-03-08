import { Routes } from '@angular/router';
import { CatalogComponent } from './components/catalog/catalog.component';
import { ModDetailComponent } from './components/mod-detail/mod-detail.component';
import { CartComponent } from './components/cart/cart.component';
import { CheckoutComponent } from './components/checkout/checkout.component';
import { UserDashboardComponent } from './components/user-dashboard/user-dashboard.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { DiscordCommunityComponent } from './components/discord-community/discord-community.component';
import { AuthCallbackComponent } from './components/auth-callback/auth-callback.component';
import { Faq } from './components/faq/faq';
import { PoliticaDevoluciones } from './components/politica-devoluciones/politica-devoluciones';
import { TerminosCondiciones } from './components/terminos-condiciones/terminos-condiciones';
import { Home } from './components/home/home';
import { Support } from './components/support/support';
import { authGuard } from './guards/auth.guard';
export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: Home },
  { path: 'catalog', component: CatalogComponent },
  { path: 'mod/:id', component: ModDetailComponent },
  { path: 'cart', component: CartComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },
  { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard] },
  { path: 'dashboard', component: UserDashboardComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [authGuard] },
  { path: 'discord', component: DiscordCommunityComponent },
  { path: 'faq', component: Faq },
  { path: 'politica-devoluciones', component: PoliticaDevoluciones },
  { path: 'terminos-condiciones', component: TerminosCondiciones },
  { path: 'support', component: Support },
  { path: '**', redirectTo: '/home' }
];
