import httpClient from '@/api/httpClient'
import type { ApiResponse } from '@/types/api/common'
import type { AuthResponseData, LoginRequest, RegisterRequest } from '@/types/api/auth'

export const register = (payload: RegisterRequest): Promise<ApiResponse<AuthResponseData>> => {
  return httpClient.post('/api/auth/register', payload).then((res) => res.data)
}

export const login = (payload: LoginRequest): Promise<ApiResponse<AuthResponseData>> => {
  return httpClient.post('/api/auth/login', payload).then((res) => res.data)
}
