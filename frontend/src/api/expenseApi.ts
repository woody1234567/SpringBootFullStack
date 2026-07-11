import httpClient from '@/api/httpClient'
import type { ApiResponse, PageResponse } from '@/types/api/common'
import type { CreateExpenseRequest, ExpenseSearchParams, UpdateExpenseRequest } from '@/types/api/expense'
import type { Expense } from '@/types/domain/expense'

export const createExpense = (payload: CreateExpenseRequest): Promise<ApiResponse<Expense>> => {
  return httpClient.post('/api/expenses', payload).then((res) => res.data)
}

export const updateExpense = (
  expenseId: number,
  payload: UpdateExpenseRequest,
): Promise<ApiResponse<Expense>> => {
  return httpClient.put(`/api/expenses/${expenseId}`, payload).then((res) => res.data)
}

export const deleteExpense = (expenseId: number): Promise<ApiResponse<void>> => {
  return httpClient.delete(`/api/expenses/${expenseId}`).then((res) => res.data)
}

export const getExpense = (expenseId: number): Promise<ApiResponse<Expense>> => {
  return httpClient.get(`/api/expenses/${expenseId}`).then((res) => res.data)
}

export const searchExpenses = (
  params: ExpenseSearchParams,
): Promise<ApiResponse<PageResponse<Expense>>> => {
  return httpClient.get('/api/expenses', { params }).then((res) => res.data)
}
