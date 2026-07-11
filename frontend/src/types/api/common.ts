export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  errorCode?: string
  errors?: string[]
}

export interface PageResponse<T> {
  content: T[]
  totalCount: number
  page: number
  pageSize: number
}
