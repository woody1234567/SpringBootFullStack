<script setup lang="ts">
import { Message } from 'view-ui-plus'
import { downloadTextFile } from '@/utils/downloadFile'

const SAMPLE_CSV_CONTENT = [
  'expense_date,amount,category,invoice_number,note',
  '2026-07-01,150.00,餐飲,INV-0001,午餐',
  '2026-07-02,899.50,旅費,INV-0002,住宿',
  '2026-07-03,45.00,餐飲,INV-0003,咖啡',
].join('\r\n')

const emit = defineEmits<{
  upload: [file: File]
}>()

const handleBeforeUpload = (file: File) => {
  if (!file.name.toLowerCase().endsWith('.csv')) {
    Message.error('只接受 .csv 檔案')
    return false
  }
  emit('upload', file)
  return false
}

const handleDownloadSample = () => {
  downloadTextFile('expense_import_sample.csv', SAMPLE_CSV_CONTENT, 'text/csv;charset=utf-8;')
}
</script>

<template>
  <div class="csv-upload-panel">
    <div class="csv-upload-panel__actions">
      <Upload action="" :before-upload="handleBeforeUpload" :show-upload-list="false" accept=".csv">
        <Button icon="ios-cloud-upload-outline">選擇 CSV 檔案上傳</Button>
      </Upload>
      <Button icon="ios-download-outline" @click="handleDownloadSample">下載範例檔</Button>
    </div>

    <Alert type="info" show-icon class="csv-upload-panel__spec">
      <template #desc>
        <p class="csv-upload-panel__spec-intro">
          CSV 標題列（不分大小寫）須包含以下 5 個欄位，檔案編碼須為 UTF-8：
        </p>
        <table class="csv-upload-panel__spec-table">
          <thead>
            <tr>
              <th>欄位名稱</th>
              <th>必填</th>
              <th>格式規範</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>expense_date</code></td>
              <td>必填</td>
              <td>日期，格式為 yyyy-MM-dd（例：2026-07-11）</td>
            </tr>
            <tr>
              <td><code>amount</code></td>
              <td>必填</td>
              <td>數字，必須大於 0，最多 2 位小數（例：123.45）</td>
            </tr>
            <tr>
              <td><code>category</code></td>
              <td>必填</td>
              <td>須符合系統中既有且啟用中的類別名稱（不分大小寫）</td>
            </tr>
            <tr>
              <td><code>invoice_number</code></td>
              <td>選填</td>
              <td>最長 20 字元；同一檔案內與歷史紀錄皆不可重複</td>
            </tr>
            <tr>
              <td><code>note</code></td>
              <td>選填</td>
              <td>最長 500 字元，自由文字備註</td>
            </tr>
          </tbody>
        </table>
        <p class="csv-upload-panel__spec-note">
          範例：<code>expense_date,amount,category,invoice_number,note</code><br />
          任一資料列驗證失敗，整批資料都不會匯入，請修正後重新上傳。
        </p>
      </template>
    </Alert>
  </div>
</template>

<style scoped>
.csv-upload-panel__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.csv-upload-panel__spec {
  margin-top: 12px;
}

.csv-upload-panel__spec-intro {
  margin-bottom: 8px;
}

.csv-upload-panel__spec-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-bottom: 8px;
}

.csv-upload-panel__spec-table th,
.csv-upload-panel__spec-table td {
  padding: 4px 8px;
  border-bottom: 1px solid #e8eaec;
  text-align: left;
  vertical-align: top;
}

.csv-upload-panel__spec-table code {
  font-size: 12px;
}

.csv-upload-panel__spec-note {
  color: #808695;
  font-size: 13px;
}
</style>
