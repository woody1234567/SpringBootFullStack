import { ref } from 'vue'

export function usePagination(initialPageSize = 20) {
  const page = ref(1)
  const pageSize = ref(initialPageSize)
  const totalCount = ref(0)

  const reset = () => {
    page.value = 1
  }

  return {
    page,
    pageSize,
    totalCount,
    reset,
  }
}
