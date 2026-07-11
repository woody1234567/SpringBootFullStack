import type { User } from '@/types/domain/user'

export interface RegisterRequest {
  email: string
  password: string
  displayName?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponseData {
  token: string
  user: User
}
