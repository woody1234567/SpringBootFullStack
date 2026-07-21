<script setup lang="ts">
import { ref, watch } from 'vue'
import { Message } from 'view-ui-plus'
import type { Category } from '@/types/domain/category'
import type { Expense } from '@/types/domain/expense'
import type { ExpenseForm } from '@/types/form/expenseForm'
import type { ViewUiFormInstance, ViewUiFormRules } from '@/types/form/viewUiForm'
import { useApiError } from '@/composables/useApiError'

interface Props {
  modelValue: boolean
  categories: Category[]
  editing: Expense | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  submit: [payload: ExpenseForm]
}>()

const { getErrorMessage } = useApiError()

const formRef = ref<ViewUiFormInstance>()
const submitting = ref(false)

const emptyForm = (): ExpenseForm => ({
  expenseDate: '',
  amount: null,
  categoryId: null,
  invoiceNumber: '',
  note: '',
})

const form = ref<ExpenseForm>(emptyForm())

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      return
    }
    form.value = props.editing
      ? {
          expenseDate: props.editing.expenseDate,
          amount: props.editing.amount,
          categoryId: props.editing.categoryId,
          invoiceNumber: props.editing.invoiceNumber ?? '',
          note: props.editing.note ?? '',
        }
      : emptyForm()
  },
)

const rules: ViewUiFormRules = {
  expenseDate: [{ required: true, message: '請選擇日期', trigger: 'change' }],
  amount: [{ required: true, type: 'number', message: '請輸入金額', trigger: 'blur' }],
  categoryId: [{ required: true, type: 'string', message: '請選擇分類', trigger: 'change' }],
}

const handleOk = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    submitting.value = true
    try {
      emit('submit', { ...form.value })
    } catch (error) {
      Message.error(getErrorMessage(error))
    } finally {
      submitting.value = false
    }
  })
}

const handleCancel = () => {
  emit('update:modelValue', false)
}
</script>

<template>
  <Modal
    :model-value="modelValue"
    :title="editing ? '編輯消費紀錄' : '新增消費紀錄'"
    :loading="submitting"
    @update:model-value="(value: boolean) => emit('update:modelValue', value)"
    @on-ok="handleOk"
    @on-cancel="handleCancel"
  >
    <Form ref="formRef" :model="form" :rules="rules" label-position="top">
      <FormItem label="日期" prop="expenseDate">
        <Input v-model="form.expenseDate" placeholder="yyyy-MM-dd" />
      </FormItem>
      <FormItem label="金額" prop="amount">
        <InputNumber v-model="form.amount" :min="0.01" style="width: 100%" />
      </FormItem>
      <FormItem label="分類" prop="categoryId">
        <Select v-model="form.categoryId" placeholder="請選擇分類" transfer>
          <Option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
            {{ category.name }}
          </Option>
        </Select>
      </FormItem>
      <FormItem label="發票號碼">
        <Input v-model="form.invoiceNumber" placeholder="選填" />
      </FormItem>
      <FormItem label="備註">
        <Input v-model="form.note" type="textarea" placeholder="選填" />
      </FormItem>
    </Form>
  </Modal>
</template>
