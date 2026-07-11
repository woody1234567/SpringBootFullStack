import { defineStore } from 'pinia'
import * as authApi from '@/api/authApi'
import type { LoginRequest, RegisterRequest } from '@/types/api/auth'
import type { User } from '@/types/domain/user'

const TOKEN_STORAGE_KEY = 'expense_tracker_token'
const USER_STORAGE_KEY = 'expense_tracker_user'

interface AuthState {
  user: User | null
  token: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: JSON.parse(localStorage.getItem(USER_STORAGE_KEY) ?? 'null'),
    token: localStorage.getItem(TOKEN_STORAGE_KEY),
  }),

  getters: {
    isAuthenticated: (state) => state.token !== null && state.user !== null,
  },

  actions: {
    async login(payload: LoginRequest): Promise<void> {
      const response = await authApi.login(payload)
      this.setSession(response.data.token, response.data.user)
    },

    async register(payload: RegisterRequest): Promise<void> {
      const response = await authApi.register(payload)
      this.setSession(response.data.token, response.data.user)
    },

    logout(): void {
      this.user = null
      this.token = null
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      localStorage.removeItem(USER_STORAGE_KEY)
    },

    setSession(token: string, user: User): void {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_STORAGE_KEY, token)
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
    },
  },
})
