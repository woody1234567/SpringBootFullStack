<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Message, Modal as ModalApi } from 'view-ui-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import ExpenseFilterBar from '@/components/expense/ExpenseFilterBar.vue'
import ExpenseTable from '@/components/expense/ExpenseTable.vue'
import ExpenseFormModal from '@/components/expense/ExpenseFormModal.vue'
import * as expenseApi from '@/api/expenseApi'
import * as categoryApi from '@/api/categoryApi'
import { usePagination } from '@/composables/usePagination'
import { useExpenseFilters } from '@/composables/useExpenseFilters'
import { useApiError } from '@/composables/useApiError'
import { toIsoDate } from '@/utils/date'
import type { Expense } from '@/types/domain/expense'
import type { Category } from '@/types/domain/category'
import type { ExpenseForm } from '@/types/form/expenseForm'

const { page, pageSize, totalCount, reset: resetPagination } = usePagination()
const { dateFrom, dateTo, categoryId, reset: resetFilters } = useExpenseFilters()
const { getErrorMessage } = useApiError()

const expenses = ref<Expense[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const modalVisible = ref(false)
const editingExpense = ref<Expense | null>(null)

const loadCategories = async () => {
  const response = await categoryApi.getCategories()
  categories.value = response.data
}

const loadExpenses = async () => {
  loading.value = true
  try {
    const response = await expenseApi.searchExpenses({
      dateFrom: toIsoDate(dateFrom.value),
      dateTo: toIsoDate(dateTo.value),
      categoryId: categoryId.value ?? undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    expenses.value = response.data.content
    totalCount.value = response.data.totalCount
  } catch (error) {
    Message.error(getErrorMessage(error))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  resetPagination()
  loadExpenses()
}

const handleResetFilters = () => {
  resetFilters()
  handleSearch()
}

const handlePageChange = (newPage: number) => {
  page.value = newPage
  loadExpenses()
}

const openCreateModal = () => {
  editingExpense.value = null
  modalVisible.value = true
}

const openEditModal = (expense: Expense) => {
  editingExpense.value = expense
  modalVisible.value = true
}

const handleSubmit = async (form: ExpenseForm) => {
  if (form.amount === null || form.categoryId === null) {
    return
  }

  const payload = {
    expenseDate: form.expenseDate,
    amount: form.amount,
    categoryId: form.categoryId,
    invoiceNumber: form.invoiceNumber || undefined,
    note: form.note || undefined,
  }

  try {
    if (editingExpense.value) {
      await expenseApi.updateExpense(editingExpense.value.expenseId, payload)
      Message.success('消費紀錄已更新')
    } else {
      await expenseApi.createExpense(payload)
      Message.success('消費紀錄已新增')
    }
    modalVisible.value = false
    loadExpenses()
  } catch (error) {
    Message.error(getErrorMessage(error))
  }
}

const handleDelete = (expense: Expense) => {
  ModalApi.confirm({
    title: '刪除消費紀錄',
    content: `確定要刪除 ${expense.expenseDate} 的紀錄嗎？`,
    onOk: async () => {
      try {
        await expenseApi.deleteExpense(expense.expenseId)
        Message.success('消費紀錄已刪除')
        loadExpenses()
      } catch (error) {
        Message.error(getErrorMessage(error))
      }
    },
  })
}

onMounted(async () => {
  await loadCategories()
  await loadExpenses()
})
</script>

<template>
  <div>
    <AppHeader />
    <div class="expense-page">
      <div class="expense-page__toolbar">
        <ExpenseFilterBar
          v-model:date-from="dateFrom"
          v-model:date-to="dateTo"
          v-model:category-id="categoryId"
          :categories="categories"
          @search="handleSearch"
          @reset="handleResetFilters"
        />
        <Button type="primary" @click="openCreateModal">新增消費紀錄</Button>
      </div>

      <ExpenseTable :expenses="expenses" :loading="loading" @edit="openEditModal" @delete="handleDelete" />

      <div class="expense-page__pagination">
        <Page
          :total="totalCount"
          :current="page"
          :page-size="pageSize"
          show-total
          @on-change="handlePageChange"
        />
      </div>
    </div>

    <ExpenseFormModal
      v-model="modalVisible"
      :categories="categories"
      :editing="editingExpense"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.expense-page {
  padding: 24px;
}

.expense-page__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
}

.expense-page__pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
