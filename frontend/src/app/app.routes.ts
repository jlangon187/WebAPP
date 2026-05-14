import { Routes } from '@angular/router';
import { CatalogComponent } from './components/catalog/catalog.component';
import { ModDetailComponent } from './components/mod-detail/mod-detail.component';
import { CartComponent } from './components/cart/cart.component';
import { CheckoutComponent } from './components/checkout/checkout.component';
import { UserDashboardComponent } from './components/user-dashboard/user-dashboard.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { AdminModsManagerComponent } from './components/admin-mods-manager/admin-mods-manager.component';
import { AdminTicketsManagerComponent } from './components/admin-tickets-manager/admin-tickets-manager.component';
import { AdminUsersManagerComponent } from './components/admin-users-manager/admin-users-manager.component';
import { AdminEncryptionJobsManagerComponent } from './components/admin-encryption-jobs-manager/admin-encryption-jobs-manager.component';
import { DiscordCommunityComponent } from './components/discord-community/discord-community.component';
import { AuthCallbackComponent } from './components/auth-callback/auth-callback.component';
import { Faq } from './components/faq/faq';
import { PoliticaDevoluciones } from './components/politica-devoluciones/politica-devoluciones';
import { TerminosCondiciones } from './components/terminos-condiciones/terminos-condiciones';
import { Home } from './components/home/home';
import { Support } from './components/support/support';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password';
import { ResetPasswordComponent } from './components/reset-password/reset-password';
import { CompleteProfileComponent } from './components/complete-profile/complete-profile';
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
  { path: 'complete-profile', component: CompleteProfileComponent, canActivate: [authGuard] },
  { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard] },
  { path: 'dashboard', component: UserDashboardComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [authGuard] },
  { path: 'admin/mods', component: AdminModsManagerComponent, canActivate: [authGuard] },
  { path: 'admin/tickets', component: AdminTicketsManagerComponent, canActivate: [authGuard] },
  { path: 'admin/users', component: AdminUsersManagerComponent, canActivate: [authGuard] },
  { path: 'admin/encryption-jobs', component: AdminEncryptionJobsManagerComponent, canActivate: [authGuard] },
  { path: 'discord', component: DiscordCommunityComponent },
  { path: 'faq', component: Faq },
  { path: 'politica-devoluciones', component: PoliticaDevoluciones },
  { path: 'terminos-condiciones', component: TerminosCondiciones },
  { path: 'support', component: Support, canActivate: [authGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: '**', redirectTo: '/home' }
];
