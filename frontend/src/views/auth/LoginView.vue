<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from 'view-ui-plus'
import { useAuth } from '@/composables/useAuth'
import { useApiError } from '@/composables/useApiError'
import type { LoginForm } from '@/types/form/loginForm'
import type { ViewUiFormInstance, ViewUiFormRules } from '@/types/form/viewUiForm'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()
const { getErrorMessage } = useApiError()

const formRef = ref<ViewUiFormInstance>()
const submitting = ref(false)

const form = reactive<LoginForm>({
  email: '',
  password: '',
})

const rules: ViewUiFormRules = {
  email: [
    { required: true, message: 'Email is required', trigger: 'blur' },
    { type: 'email', message: 'Email must be valid', trigger: 'blur' },
  ],
  password: [{ required: true, message: 'Password is required', trigger: 'blur' }],
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid || submitting.value) {
      return
    }

    submitting.value = true
    try {
      await login({ email: form.email, password: form.password })
      const redirect = (route.query.redirect as string) || '/expenses'
      router.push(redirect)
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
      <h2>登入</h2>
      <Form ref="formRef" :model="form" :rules="rules" @submit.native.prevent="handleSubmit">
        <FormItem label="Email" prop="email">
          <Input v-model="form.email" placeholder="you@example.com" />
        </FormItem>
        <FormItem label="密碼" prop="password">
          <Input v-model="form.password" type="password" password placeholder="********" />
        </FormItem>
        <FormItem>
          <Button type="primary" long :loading="submitting" :disabled="submitting" @click="handleSubmit">
            登入
          </Button>
        </FormItem>
      </Form>
      <p class="auth-switch">
        還沒有帳號？<router-link to="/register">註冊</router-link>
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
  width: 360px;
}

.auth-switch {
  margin-top: 12px;
  text-align: center;
}
</style>
