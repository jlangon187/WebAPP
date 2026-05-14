import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Categoria {
  id: number;
  nombre: string;
}

export interface Mod {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  version: string;
  archivoOriginal?: string;
  categoria?: Categoria | null;
  destacadoHome?: boolean;
  ordenShowroom?: number | null;
  youtubeUrl?: string | null;
  carpetaBaseMod?: string | null;
}

export interface Comentario {
  id: number;
  puntuacion: number;
  mensaje: string;
  creadoEn: string;
  usuarioNombre: string;
  usuarioId: number;
}

export interface ModRatingSummary {
  modId: number;
  avgPuntuacion: number;
  totalComentarios: number;
}

export interface ModPurchaseStats {
  modId: number;
  totalPurchases: number;
  purchasesLast30Days: number;
}

export interface AdminPurchase {
  id: number;
  fecha: string;
  precioPagado: number;
  metodoPago: string;
  guidCompra: string;
  mod: {
    id: number;
    nombre: string;
    version: string;
    archivoOriginal?: string;
  };
}

export interface PrepareDownloadResponse {
  jobId: number;
  status: 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED' | string;
  message?: string;
  downloadToken?: string | null;
}

export interface PaymentSessionResponse {
  provider: string;
  redirectUrl: string;
  externalId: string;
  message: string;
}

export interface PaymentConfirmResponse {
  created: number;
  message: string;
}

export interface AdminUser {
  id: number;
  nombre: string;
  email: string;
  guid: string;
  guidValid?: boolean;
  profileCompleted?: boolean;
  rol: 'invitado' | 'registrado' | 'admin' | string;
  activo: boolean;
  creadoEn: string;
  purchasesCount: number;
  ticketsCount?: number;
  totalSpent: number;
  lastPurchaseAt?: string | null;
  purchases: AdminPurchase[];
}

@Injectable({
  providedIn: 'root'
})
export class ModService {

  private apiUrl = '/api';

  constructor(private http: HttpClient) { }

  getCatalog(): Observable<Mod[]> {
    return this.http.get<Mod[]>(`${this.apiUrl}/mods/catalog`);
  }

  getShowroomMods(): Observable<Mod[]> {
    return this.http.get<Mod[]>(`${this.apiUrl}/mods/showroom`);
  }

  getCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.apiUrl}/categorias`);
  }

  getHomeImages(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/mods/home-images`, { headers: this.getHeaders() });
  }

  getModDetails(id: number): Observable<Mod> {
    return this.http.get<Mod>(`${this.apiUrl}/mods/detail/${id}`);
  }

  purchaseMod(modId: number, metodoPago: string): Observable<any> {
    const payload = { modId, metodoPago };
    return this.http.post(`${this.apiUrl}/compras/checkout`, payload, { responseType: 'text' });
  }

  createPaymentSession(provider: 'stripe' | 'paypal', modIds: number[]): Observable<PaymentSessionResponse> {
    return this.http.post<PaymentSessionResponse>(`${this.apiUrl}/payments/create-session`, { provider, modIds }, { headers: this.getHeaders() });
  }

  confirmPayment(provider: 'stripe' | 'paypal', externalId: string, modIds: number[]): Observable<PaymentConfirmResponse> {
    return this.http.post<PaymentConfirmResponse>(`${this.apiUrl}/payments/confirm`, { provider, externalId, modIds }, { headers: this.getHeaders() });
  }

  getDownloadUrl(modId: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/descargas/${modId}`, { responseType: 'text' });
  }

  prepareDownload(modId: number): Observable<PrepareDownloadResponse> {
    return this.http.post<PrepareDownloadResponse>(`${this.apiUrl}/descargas/${modId}/prepare`, {}, { headers: this.getHeaders() });
  }

  getDownloadJobStatus(jobId: number): Observable<PrepareDownloadResponse> {
    return this.http.get<PrepareDownloadResponse>(`${this.apiUrl}/descargas/jobs/${jobId}`, { headers: this.getHeaders() });
  }

  getDownloadFileUrl(downloadToken: string): string {
    return `${this.apiUrl}/descargas/file/${downloadToken}`;
  }

  downloadPreparedFile(downloadToken: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.apiUrl}/descargas/file/${downloadToken}`, {
      headers: this.getHeaders(),
      responseType: 'blob',
      observe: 'response'
    });
  }

  getMyPurchases(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/compras/mis-compras`);
  }

  private getHeaders(): HttpHeaders {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  }

  createMod(mod: Partial<Mod>): Observable<Mod> {
    return this.http.post<Mod>(`${this.apiUrl}/mods`, mod, { headers: this.getHeaders() });
  }

  updateMod(id: number, mod: Partial<Mod>): Observable<Mod> {
    return this.http.put<Mod>(`${this.apiUrl}/mods/${id}`, mod, { headers: this.getHeaders() });
  }

  deleteMod(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/mods/${id}`, { headers: this.getHeaders() });
  }

  getComentarios(modId: number): Observable<Comentario[]> {
    return this.http.get<Comentario[]>(`${this.apiUrl}/mods/${modId}/comentarios`);
  }

  getRatingsSummary(): Observable<ModRatingSummary[]> {
    return this.http.get<ModRatingSummary[]>(`${this.apiUrl}/mods/ratings`);
  }

  getPurchaseStats(): Observable<ModPurchaseStats[]> {
    return this.http.get<ModPurchaseStats[]>(`${this.apiUrl}/mods/purchase-stats`);
  }

  createComentario(modId: number, puntuacion: number, mensaje: string): Observable<Comentario> {
    return this.http.post<Comentario>(`${this.apiUrl}/mods/${modId}/comentarios`, { puntuacion, mensaje }, { headers: this.getHeaders() });
  }

  deleteComentario(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/admin/comentarios/${id}`, { headers: this.getHeaders() });
  }

  getMyTickets(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tickets/mis-tickets`, { headers: this.getHeaders() });
  }

  getMyComentarios(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/comentarios/mis`, { headers: this.getHeaders() });
  }

  getAdminUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.apiUrl}/admin/users`, { headers: this.getHeaders() });
  }

  updateAdminUser(userId: number, payload: Partial<AdminUser> & { password?: string; activo?: boolean }): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.apiUrl}/admin/users/${userId}`, payload, { headers: this.getHeaders() });
  }

  updateAdminPurchaseGuid(userId: number, purchaseId: number, guidCompra: string): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.apiUrl}/admin/users/${userId}/purchases/${purchaseId}/guid`, { guidCompra }, { headers: this.getHeaders() });
  }

  resendAdminPurchaseDownloadEmail(userId: number, purchaseId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/users/${userId}/purchases/${purchaseId}/resend-download-email`, {}, { headers: this.getHeaders(), responseType: 'text' as 'json' });
  }

  prepareAdminPurchaseDownload(userId: number, purchaseId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/users/${userId}/purchases/${purchaseId}/prepare-download`, {}, { headers: this.getHeaders(), responseType: 'text' as 'json' });
  }
}
