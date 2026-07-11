<script setup lang="ts">
import { ref } from 'vue'
import { Message } from 'view-ui-plus'

const emit = defineEmits<{
  upload: [file: File]
}>()

const uploading = ref(false)

defineExpose({
  setUploading: (value: boolean) => {
    uploading.value = value
  },
})

const handleBeforeUpload = (file: File) => {
  if (!file.name.toLowerCase().endsWith('.csv')) {
    Message.error('只接受 .csv 檔案')
    return false
  }
  emit('upload', file)
  return false
}
</script>

<template>
  <div class="csv-upload-panel">
    <Upload action="" :before-upload="handleBeforeUpload" :show-upload-list="false" accept=".csv">
      <Button icon="ios-cloud-upload-outline" :loading="uploading">選擇 CSV 檔案上傳</Button>
    </Upload>
    <p class="csv-upload-panel__hint">
      欄位需包含：expense_date, amount, category, invoice_number, note（標題列必須存在）
    </p>
  </div>
</template>

<style scoped>
.csv-upload-panel__hint {
  margin-top: 8px;
  color: #808695;
  font-size: 13px;
}
</style>
