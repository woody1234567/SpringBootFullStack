export interface CreateExpenseRequest {
  expenseDate: string
  amount: number
  categoryId: string
  invoiceNumber?: string | null
  note?: string | null
}

export type UpdateExpenseRequest = CreateExpenseRequest

export interface ExpenseSearchParams {
  dateFrom?: string
  dateTo?: string
  categoryId?: string
  page: number
  pageSize: number
}
