import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    if (error.response) {
      const { status, config, data } = error.response
      const requestUrl = config?.url || ''
      const isAuthCheck = requestUrl.includes('/auth/me')
      const isAuthPage = ['/login', '/register'].some((path) => window.location.pathname.startsWith(path))
      const isAuthEndpoint = ['/auth/login', '/auth/register'].some((path) => requestUrl.includes(path))
      const serverMessage = data?.message

      switch (status) {
        case 401:
          // 如果是检查用户信息的请求，不跳转（允许未登录状态）
          if (isAuthCheck) {
            break
          }
          // 如果是登录/注册接口，不跳转
          if (isAuthEndpoint) {
            break
          }
          // 如果已经在登录/注册页面，不跳转
          if (isAuthPage) {
            break
          }
          // 其他情况才跳转到登录页
          ElMessage.error(serverMessage || '请先登录')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error(serverMessage || '没有访问权限')
          break
        case 404:
          ElMessage.error(serverMessage || '请求的资源不存在')
          break
        default:
          ElMessage.error(serverMessage || error.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export default request
