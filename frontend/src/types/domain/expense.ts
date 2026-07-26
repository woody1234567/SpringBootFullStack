export interface Expense {
  expenseId: string
  expenseDate: string
  amount: number
  categoryId: string
  categoryName: string
  invoiceNumber: string | null
  note: string | null
  createTime: string
  updateTime: string
}
