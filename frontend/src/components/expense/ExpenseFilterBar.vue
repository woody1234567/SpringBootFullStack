<script setup lang="ts">
import type { Category } from '@/types/domain/category'

interface Props {
  categories: Category[]
}

defineProps<Props>()

const dateFrom = defineModel<Date | null>('dateFrom', { default: null })
const dateTo = defineModel<Date | null>('dateTo', { default: null })
const categoryId = defineModel<number | null>('categoryId', { default: null })

const emit = defineEmits<{
  search: []
  reset: []
}>()
</script>

<template>
  <div class="expense-filter-bar">
    <DatePicker
      v-model="dateFrom"
      type="date"
      placeholder="起始日期"
      transfer
      style="width: 160px"
    />
    <DatePicker
      v-model="dateTo"
      type="date"
      placeholder="結束日期"
      transfer
      style="width: 160px"
    />
    <Select v-model="categoryId" placeholder="全部分類" clearable transfer style="width: 160px">
      <Option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
        {{ category.name }}
      </Option>
    </Select>
    <Button type="primary" @click="emit('search')">查詢</Button>
    <Button @click="emit('reset')">重設</Button>
  </div>
</template>

<style scoped>
.expense-filter-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
</style>
