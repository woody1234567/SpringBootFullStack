export interface CreateExpenseRequest {
  expenseDate: string
  amount: number
  categoryId: number
  invoiceNumber?: string | null
  note?: string | null
}

export type UpdateExpenseRequest = CreateExpenseRequest

export interface ExpenseSearchParams {
  dateFrom?: string
  dateTo?: string
  categoryId?: number
  page: number
  pageSize: number
}
