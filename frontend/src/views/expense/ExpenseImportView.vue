<script setup lang="ts">
import { ref } from 'vue'
import { Message } from 'view-ui-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import CsvUploadPanel from '@/components/import/CsvUploadPanel.vue'
import ImportResultTable from '@/components/import/ImportResultTable.vue'
import * as importApi from '@/api/importApi'
import { useApiError } from '@/composables/useApiError'
import type { ImportResultData } from '@/types/api/import'

const { getErrorMessage } = useApiError()

const uploading = ref(false)
const result = ref<ImportResultData | null>(null)

const handleUpload = async (file: File) => {
  uploading.value = true
  result.value = null
  try {
    const response = await importApi.importExpenses(file)
    result.value = response.data
  } catch (error) {
    Message.error(getErrorMessage(error))
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div>
    <AppHeader />
    <div class="import-page">
      <Card>
        <h2>CSV 批次匯入</h2>
        <p class="import-page__note">
          整批匯入為單一交易：任何一筆資料驗證失敗，整批都不會寫入。
        </p>
        <CsvUploadPanel @upload="handleUpload" />
        <Spin v-if="uploading" fix />
        <ImportResultTable v-if="result" :result="result" />
      </Card>
    </div>
  </div>
</template>

<style scoped>
.import-page {
  padding: 24px;
}

.import-page__note {
  color: #808695;
  margin-bottom: 16px;
}
</style>
