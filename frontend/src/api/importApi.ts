import httpClient from '@/api/httpClient'
import type { ApiResponse } from '@/types/api/common'
import type { ImportResultData } from '@/types/api/import'

export const importExpenses = (file: File): Promise<ApiResponse<ImportResultData>> => {
  const formData = new FormData()
  formData.append('file', file)

  return httpClient
    .post('/api/imports/expenses', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data)
}
