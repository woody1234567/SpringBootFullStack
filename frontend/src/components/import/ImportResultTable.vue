<script setup lang="ts">
import type { ImportResultData } from '@/types/api/import'

interface Props {
  result: ImportResultData
}

defineProps<Props>()

const hasFieldErrors = (fieldErrors: Record<string, string[]>) => Object.keys(fieldErrors ?? {}).length > 0
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
          <td>{{ failure.rowNumber === 0 ? '-' : failure.rowNumber }}</td>
          <td>
            <div>{{ failure.message }}</div>
            <dl v-if="hasFieldErrors(failure.fieldErrors)" class="import-result__field-errors">
              <template v-for="(messages, field) in failure.fieldErrors" :key="field">
                <dt>{{ field }}</dt>
                <dd>{{ messages.join('；') }}</dd>
              </template>
            </dl>
          </td>
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
  vertical-align: top;
}

.import-result__field-errors {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 4px 8px;
  margin: 6px 0 0;
  color: #515a6e;
  font-size: 13px;
}

.import-result__field-errors dt {
  font-weight: 600;
}

.import-result__field-errors dd {
  margin: 0;
}
</style>
