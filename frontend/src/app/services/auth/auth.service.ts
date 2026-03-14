import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'http://localhost:8080/api/auth';
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const user = localStorage.getItem('user');
    if (user) {
      const normalized = this.normalizeUser(JSON.parse(user));
      localStorage.setItem('user', JSON.stringify(normalized));
      this.currentUserSubject.next(normalized);
    }
  }

  register(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data, { responseType: 'text' });
  }

  login(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, data).pipe(
      tap((res: any) => {
        if (res && res.token) {
          const normalized = this.normalizeUser(res);
          localStorage.setItem('user', JSON.stringify(normalized));
          this.currentUserSubject.next(normalized);
        }
      })
    );
  }

  logout() {
    localStorage.removeItem('user');
    this.currentUserSubject.next(null);
    window.location.href = '/login';
  }

  updateProfile(data: any): Observable<any> {
    const headers = { 'Authorization': `Bearer ${this.getToken()}` };
    return this.http.put(`${this.apiUrl}/profile`, data, { headers });
  }

  setExternalLogin(res: any) {
    if (res && res.token) {
      const normalized = this.normalizeUser(res);
      localStorage.setItem('user', JSON.stringify(normalized));
      this.currentUserSubject.next(normalized);
    }
  }

  getToken(): string {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    return user?.token || '';
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): any {
    return this.currentUserSubject.value;
  }

  private normalizeUser(user: any): any {
    if (!user || !user.token) {
      return user;
    }

    const email = user.email || user.sub || this.getEmailFromJwt(user.token) || '';
    return { ...user, email };
  }

  private getEmailFromJwt(token: string): string {
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return '';
      }
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(base64));
      return decoded?.sub || '';
    } catch {
      return '';
    }
  }
}
