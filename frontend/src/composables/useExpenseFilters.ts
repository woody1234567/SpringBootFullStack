import { ref } from 'vue'

export function useExpenseFilters() {
  const dateFrom = ref<Date | null>(null)
  const dateTo = ref<Date | null>(null)
  const categoryId = ref<string | null>(null)

  const reset = () => {
    dateFrom.value = null
    dateTo.value = null
    categoryId.value = null
  }

  return {
    dateFrom,
    dateTo,
    categoryId,
    reset,
  }
}
