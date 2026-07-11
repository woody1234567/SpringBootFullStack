<script setup lang="ts">
import type { Expense } from '@/types/domain/expense'

interface Props {
  expenses: Expense[]
  loading: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  edit: [expense: Expense]
  delete: [expense: Expense]
}>()
</script>

<template>
  <div class="expense-table">
    <Spin v-if="loading" fix />

    <Alert v-else-if="expenses.length === 0" show-icon>
      沒有符合條件的消費紀錄
    </Alert>

    <table v-else class="expense-table__table">
      <thead>
        <tr>
          <th>日期</th>
          <th>分類</th>
          <th>金額</th>
          <th>發票號碼</th>
          <th>備註</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="expense in expenses" :key="expense.expenseId">
          <td>{{ expense.expenseDate }}</td>
          <td>{{ expense.categoryName }}</td>
          <td>{{ expense.amount.toFixed(2) }}</td>
          <td>{{ expense.invoiceNumber || '-' }}</td>
          <td>{{ expense.note || '-' }}</td>
          <td class="expense-table__actions">
            <Button size="small" @click="emit('edit', expense)">編輯</Button>
            <Button size="small" type="error" @click="emit('delete', expense)">刪除</Button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.expense-table {
  position: relative;
  min-height: 120px;
}

.expense-table__table {
  width: 100%;
  border-collapse: collapse;
}

.expense-table__table th,
.expense-table__table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e8eaec;
  text-align: left;
}

.expense-table__actions {
  display: flex;
  gap: 8px;
}
</style>
