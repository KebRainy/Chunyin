import { defineStore } from 'pinia'
import { getCurrentUser, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    loading: false,
    initialized: false
  }),

  getters: {
    isLoggedIn: (state) => !!state.userInfo?.id,
    isAdmin: (state) => state.userInfo?.role === 'ADMIN',
    isSeller: (state) => state.userInfo?.role === 'SELLER'
  },

  actions: {
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      this.initialized = true  // 标记已初始化
    },

    async fetchUserInfo() {
      this.loading = true
      try {
        const res = await getCurrentUser()
        this.userInfo = res.data
      } catch (err) {
        this.userInfo = null
        return Promise.reject(err)
      } finally {
        this.loading = false
        this.initialized = true
      }
    },

    async logout() {
      await logoutApi()
      this.userInfo = null
      this.initialized = true
    }
  }
})
