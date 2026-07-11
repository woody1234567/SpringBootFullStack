<script setup lang="ts">
import type { ImportResultData } from '@/types/api/import'

interface Props {
  result: ImportResultData
}

defineProps<Props>()
</script>

<template>
  <div class="import-result">
    <Alert v-if="result.status === 'SUCCESS'" type="success" show-icon>
      匯入成功：共 {{ result.totalRows }} 筆，成功 {{ result.successCount }} 筆
    </Alert>

    <Alert v-else type="error" show-icon>
      匯入失敗：共 {{ result.totalRows }} 筆，成功 0 筆（整批未寫入，請修正後重新上傳）
    </Alert>

    <table v-if="result.failedRows.length > 0" class="import-result__table">
      <thead>
        <tr>
          <th>資料列</th>
          <th>錯誤原因</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="failure in result.failedRows" :key="failure.rowNumber">
          <td>{{ failure.rowNumber }}</td>
          <td>{{ failure.message }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.import-result {
  margin-top: 16px;
}

.import-result__table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
}

.import-result__table th,
.import-result__table td {
  padding: 8px 12px;
  border-bottom: 1px solid #e8eaec;
  text-align: left;
}
</style>
