export interface Expense {
  expenseId: number
  expenseDate: string
  amount: number
  categoryId: number
  categoryName: string
  invoiceNumber: string | null
  note: string | null
  createdAt: string
  updatedAt: string
}
