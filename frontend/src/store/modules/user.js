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

    async fetchUserInfo(force = false) {
      // 重要：如果用户已登录，直接返回，不需要重新获取
      // 这样可以避免不必要的API调用，也避免误判登录状态
      if (!force && this.userInfo && this.userInfo.id) {
        this.initialized = true
        return
      }
      
      // 如果已经在加载中，避免重复请求
      if (this.loading) {
        // 等待当前请求完成
        while (this.loading) {
          await new Promise(resolve => setTimeout(resolve, 50))
        }
        // 等待完成后，如果已经有用户信息，直接返回
        if (this.userInfo && this.userInfo.id) {
          this.initialized = true
          return
        }
      }
      
      this.loading = true
      try {
        const res = await getCurrentUser()
        this.userInfo = res.data
        this.initialized = true
      } catch (err) {
        // 只有在明确是401未授权错误时才清空用户信息
        // 其他错误（如网络错误）保留现有用户信息，避免误判
        if (err?.response?.status === 401) {
          // 401错误，说明token确实过期或无效
          this.userInfo = null
          this.initialized = true
        } else {
          // 网络错误等其他错误，如果用户之前已登录，保留登录状态
          // 不设置initialized为true，允许下次重试
          console.log('获取用户信息失败（非401），保留现有状态:', err)
          // 如果之前有用户信息，保留它
          if (this.userInfo) {
            console.log('保留现有用户信息，不因网络错误而清空')
          }
        }
        return Promise.reject(err)
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await logoutApi()
      } catch (error) {
        // 即使logout接口失败（如403），也继续执行登出逻辑
        // 因为用户可能已经登出了，但cookie还在
      }
      this.userInfo = null
      this.initialized = true
    }
  }
})
