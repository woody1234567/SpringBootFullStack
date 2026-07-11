import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/authStore'

export function useAuth() {
  const authStore = useAuthStore()
  const { user, isAuthenticated } = storeToRefs(authStore)

  return {
    user,
    isAuthenticated,
    login: authStore.login,
    register: authStore.register,
    logout: authStore.logout,
  }
}
