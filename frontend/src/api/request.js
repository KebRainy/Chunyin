import axios from 'axios'
import { ElMessage } from 'element-plus'
import pinia from '@/store'
import { useUserStore } from '@/store/modules/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true
})

// 请求重试配置
const MAX_RETRIES = 3
const RETRY_DELAY = 1000 // 1秒
const RETRY_DELAY_MULTIPLIER = 2 // 每次重试延迟翻倍

// 判断是否为连接错误（后端未启动）
const isConnectionError = (error) => {
  return (
    error.code === 'ECONNREFUSED' ||
    error.message?.includes('ECONNREFUSED') ||
    error.message?.includes('Network Error') ||
    (error.response === undefined && error.request !== undefined)
  )
}

// 判断是否应该静默处理（不显示错误消息）
const shouldSilentRetry = (config) => {
  const url = config?.url || ''
  // 对于这些接口，连接失败时静默重试，不显示错误
  const silentRetryUrls = ['/auth/me', '/recommend/posts', '/circle/posts', '/daily-question/today']
  return silentRetryUrls.some(path => url.includes(path))
}

// 请求拦截器：添加重试逻辑
request.interceptors.request.use(
  (config) => {
    // 初始化重试次数
    if (!config.__retryCount) {
      config.__retryCount = 0
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  async (error) => {
    const config = error.config || {}

    // 如果是连接错误且可以重试
    if (isConnectionError(error) && config.__retryCount < MAX_RETRIES) {
      config.__retryCount = config.__retryCount || 0
      config.__retryCount += 1

      // 计算延迟时间（指数退避）
      const delay = RETRY_DELAY * Math.pow(RETRY_DELAY_MULTIPLIER, config.__retryCount - 1)

      // 静默重试：不显示错误消息
      if (shouldSilentRetry(config)) {
        // 等待后重试
        await new Promise(resolve => setTimeout(resolve, delay))
        return request(config)
      } else {
        // 非静默重试：显示重试消息
        console.log(`请求失败，${delay}ms后重试 (${config.__retryCount}/${MAX_RETRIES}):`, config.url)
        await new Promise(resolve => setTimeout(resolve, delay))
        return request(config)
      }
    }

    // 处理其他错误
    if (error.response) {
      const { status, config, data } = error.response
      const requestUrl = config?.url || ''
      const isAuthCheck = requestUrl.includes('/auth/me')
      const isAuthPage = ['/login', '/register'].some((path) => window.location.pathname.startsWith(path))
      const isAuthEndpoint = ['/auth/login', '/auth/register'].some((path) => requestUrl.includes(path))
      const serverMessage = data?.message

      switch (status) {
        case 400:
          // 400错误通常是参数错误，不应该导致退出登录
          // 只显示错误消息，不清空登录状态
          ElMessage.error(serverMessage || '请求参数错误')
          // 不reject，避免触发其他错误处理
          return Promise.reject(error)
        case 401:
          // 重要：只有在 /auth/me 接口返回401时才清空用户信息
          // 其他接口的401错误可能是权限问题，不应该清空登录状态
          if (isAuthCheck) {
            // /auth/me 返回401，说明token确实过期或无效
            const userStore = useUserStore(pinia)
            const wasLoggedIn = !!userStore.userInfo
            
            // 清空用户信息
            userStore.userInfo = null
            userStore.initialized = true
            
            // 不显示错误消息，让路由守卫处理跳转
            break
          }
          
          // 如果是登录/注册接口，不处理
          if (isAuthEndpoint) {
            break
          }
          
          // 如果已经在登录/注册页面，不处理
          if (isAuthPage) {
            break
          }
          
          // 其他接口的401错误：可能是权限问题，不清空登录状态
          // 只显示错误消息，让用户知道需要权限
          ElMessage.error(serverMessage || '没有权限访问此资源')
          break
        case 403:
          ElMessage.error(serverMessage || '没有访问权限')
          break
        case 404:
          ElMessage.error(serverMessage || '请求的资源不存在')
          break
        default:
          // 连接错误且已重试多次，才显示错误
          if (!isConnectionError(error)) {
            ElMessage.error(serverMessage || error.message || '请求失败')
          } else if (!shouldSilentRetry(config)) {
            ElMessage.error('无法连接到服务器，请检查后端服务是否已启动')
          }
      }
    } else {
      // 连接错误且已重试多次
      if (isConnectionError(error) && config.__retryCount >= MAX_RETRIES) {
        if (!shouldSilentRetry(config)) {
          ElMessage.error('无法连接到服务器，请检查后端服务是否已启动')
        }
      } else if (!isConnectionError(error)) {
        ElMessage.error('网络错误，请稍后再试')
      }
    }
    return Promise.reject(error)
  }
)

export default request
