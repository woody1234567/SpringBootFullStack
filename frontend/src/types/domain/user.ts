export type UserRole = 'admin' | 'user'

export interface User {
  userId: string
  email: string
  displayName: string | null
  role: UserRole
}
