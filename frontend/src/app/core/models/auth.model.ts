export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;
  username: string;
  rol: string;
}
