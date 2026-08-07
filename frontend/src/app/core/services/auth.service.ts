import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse } from '../models/auth.model';

const TOKEN_KEY = 'sigac_token';
const USERNAME_KEY = 'sigac_username';
const ROL_KEY = 'sigac_rol';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(sessionStorage.getItem(TOKEN_KEY));
  private readonly usernameSignal = signal<string | null>(sessionStorage.getItem(USERNAME_KEY));
  private readonly rolSignal = signal<string | null>(sessionStorage.getItem(ROL_KEY));

  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);
  readonly username = computed(() => this.usernameSignal());
  readonly rol = computed(() => this.rolSignal());

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((response) => {
        sessionStorage.setItem(TOKEN_KEY, response.token);
        sessionStorage.setItem(USERNAME_KEY, response.username);
        sessionStorage.setItem(ROL_KEY, response.rol);
        this.tokenSignal.set(response.token);
        this.usernameSignal.set(response.username);
        this.rolSignal.set(response.rol);
      }),
    );
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USERNAME_KEY);
    sessionStorage.removeItem(ROL_KEY);
    this.tokenSignal.set(null);
    this.usernameSignal.set(null);
    this.rolSignal.set(null);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }
}
