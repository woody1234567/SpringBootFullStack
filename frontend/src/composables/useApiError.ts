import axios from 'axios'
import type { ApiResponse } from '@/types/api/common'

export function useApiError() {
  const getErrorMessage = (error: unknown): string => {
    if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
      return error.response?.data?.message ?? 'An unexpected error occurred'
    }
    return 'An unexpected error occurred'
  }

  return {
    getErrorMessage,
  }
}
