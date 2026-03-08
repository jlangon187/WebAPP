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
      this.currentUserSubject.next(JSON.parse(user));
    }
  }

  register(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data, { responseType: 'text' });
  }

  login(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, data).pipe(
      tap((res: any) => {
        if (res && res.token) {
          localStorage.setItem('user', JSON.stringify(res));
          this.currentUserSubject.next(res);
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
      localStorage.setItem('user', JSON.stringify(res));
      this.currentUserSubject.next(res);
    }
  }

  getToken(): string {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    return user?.token || '';
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
