<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { user, logout } = useAuth()

const handleLogout = () => {
  logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="app-header">
    <div class="app-header__brand">消費管家</div>
    <nav class="app-header__nav">
      <router-link to="/expenses">消費紀錄</router-link>
      <router-link to="/expenses/import">CSV 匯入</router-link>
    </nav>
    <div class="app-header__user">
      <span v-if="user">{{ user.displayName || user.email }}</span>
      <Button size="small" @click="handleLogout">登出</Button>
    </div>
  </div>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background-color: #ffffff;
  border-bottom: 1px solid #e8eaec;
}

.app-header__brand {
  font-weight: 600;
  font-size: 18px;
}

.app-header__nav {
  display: flex;
  gap: 16px;
}

.app-header__user {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
