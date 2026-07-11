<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from 'view-ui-plus'
import { useAuth } from '@/composables/useAuth'
import { useApiError } from '@/composables/useApiError'
import type { RegisterForm } from '@/types/form/registerForm'
import type { ViewUiFormInstance, ViewUiFormRules } from '@/types/form/viewUiForm'

const router = useRouter()
const { register } = useAuth()
const { getErrorMessage } = useApiError()

const formRef = ref<ViewUiFormInstance>()
const submitting = ref(false)

const form = reactive<RegisterForm>({
  email: '',
  password: '',
  confirmPassword: '',
  displayName: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('兩次輸入的密碼不一致'))
    return
  }
  callback()
}

const rules: ViewUiFormRules = {
  email: [
    { required: true, message: 'Email is required', trigger: 'blur' },
    { type: 'email', message: 'Email must be valid', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 8, message: '密碼至少需要 8 個字元', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '請再次輸入密碼', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid || submitting.value) {
      return
    }

    submitting.value = true
    try {
      await register({
        email: form.email,
        password: form.password,
        displayName: form.displayName || undefined,
      })
      router.push('/expenses')
    } catch (error) {
      Message.error(getErrorMessage(error))
    } finally {
      submitting.value = false
    }
  })
}
</script>

<template>
  <div class="auth-page">
    <Card class="auth-card">
      <h2>註冊</h2>
      <Form ref="formRef" :model="form" :rules="rules" @submit.native.prevent="handleSubmit">
        <FormItem label="Email" prop="email">
          <Input v-model="form.email" placeholder="you@example.com" />
        </FormItem>
        <FormItem label="顯示名稱">
          <Input v-model="form.displayName" placeholder="選填" />
        </FormItem>
        <FormItem label="密碼" prop="password">
          <Input v-model="form.password" type="password" password placeholder="至少 8 個字元" />
        </FormItem>
        <FormItem label="確認密碼" prop="confirmPassword">
          <Input v-model="form.confirmPassword" type="password" password />
        </FormItem>
        <FormItem>
          <Button type="primary" long :loading="submitting" :disabled="submitting" @click="handleSubmit">
            註冊
          </Button>
        </FormItem>
      </Form>
      <p class="auth-switch">
        已經有帳號？<router-link to="/login">登入</router-link>
      </p>
    </Card>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.auth-card {
  width: 400px;
}

.auth-switch {
  margin-top: 12px;
  text-align: center;
}
</style>
