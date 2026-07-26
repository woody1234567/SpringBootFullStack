export interface ImportRowError {
  rowNumber: number
  message: string
  fieldErrors: Record<string, string[]>
}

export interface ImportResultData {
  batchId: string
  totalRows: number
  successCount: number
  status: 'SUCCESS' | 'FAILED'
  failedRows: ImportRowError[]
}
