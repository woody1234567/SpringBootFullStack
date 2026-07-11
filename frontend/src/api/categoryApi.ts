import httpClient from '@/api/httpClient'
import type { ApiResponse } from '@/types/api/common'
import type { Category } from '@/types/domain/category'

export const getCategories = (): Promise<ApiResponse<Category[]>> => {
  return httpClient.get('/api/categories').then((res) => res.data)
}
