<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import type { UserRole } from '@/types/domain/user'

const router = useRouter()
const { user, logout } = useAuth()

const roleLabels: Record<UserRole, string> = {
  admin: '管理員',
  user: '一般使用者',
}

const userDisplayName = computed(() => {
  if (!user.value) {
    return ''
  }

  return user.value.displayName || user.value.email
})

const userAvatarText = computed(() => {
  const source = userDisplayName.value.trim()

  return source ? source.charAt(0).toUpperCase() : '?'
})

const userRoleLabel = computed(() => {
  if (!user.value) {
    return ''
  }

  return roleLabels[user.value.role]
})

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
    <div v-if="user" class="app-header__user">
      <Avatar class="app-header__avatar">{{ userAvatarText }}</Avatar>
      <div class="app-header__user-meta">
        <span class="app-header__user-name">{{ userDisplayName }}</span>
        <Tag class="app-header__role" size="small" color="blue">{{ userRoleLabel }}</Tag>
      </div>
      <Button size="small" @click="handleLogout">登出</Button>
    </div>
  </div>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 12px 24px;
  background-color: #ffffff;
  border-bottom: 1px solid #e8eaec;
}

.app-header__brand {
  flex: 0 0 auto;
  font-weight: 600;
  font-size: 18px;
}

.app-header__nav {
  display: flex;
  flex: 1 1 auto;
  gap: 16px;
  min-width: 0;
}

.app-header__user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.app-header__avatar {
  flex: 0 0 auto;
  background-color: #2d8cf0;
}

.app-header__user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.app-header__user-name {
  overflow: hidden;
  max-width: 180px;
  color: #17233d;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-header__role {
  flex: 0 0 auto;
  margin: 0;
}

@media (max-width: 720px) {
  .app-header {
    flex-wrap: wrap;
  }

  .app-header__nav {
    order: 3;
    flex-basis: 100%;
  }

  .app-header__user-name {
    max-width: 120px;
  }
}
</style>
