import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Mod {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  version: string;
  archivoOriginal?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ModService {

  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  getCatalog(): Observable<Mod[]> {
    return this.http.get<Mod[]>(`${this.apiUrl}/mods/catalog`);
  }

  getModDetails(id: number): Observable<Mod> {
    return this.http.get<Mod>(`${this.apiUrl}/mods/${id}`);
  }

  purchaseMod(modId: number, metodoPago: string): Observable<any> {
    const payload = { modId, metodoPago };
    return this.http.post(`${this.apiUrl}/compras/checkout`, payload, { responseType: 'text' });
  }

  getDownloadUrl(modId: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/descargas/${modId}`, { responseType: 'text' });
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
}
