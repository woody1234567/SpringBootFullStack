export interface ImportRowError {
  rowNumber: number
  message: string
}

export interface ImportResultData {
  batchId: string
  totalRows: number
  successCount: number
  status: 'SUCCESS' | 'FAILED'
  failedRows: ImportRowError[]
}
