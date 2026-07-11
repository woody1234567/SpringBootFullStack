export interface ImportRowError {
  rowNumber: number
  message: string
}

export interface ImportResultData {
  batchId: number
  totalRows: number
  successCount: number
  status: 'SUCCESS' | 'FAILED'
  failedRows: ImportRowError[]
}
