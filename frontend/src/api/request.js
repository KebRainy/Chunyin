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
      const serverMessage = data?.message

      switch (status) {
        case 401:
          if (isAuthCheck) {
            break
          }
          ElMessage.error(serverMessage || '请先登录')
          if (!isAuthPage) {
            window.location.href = '/login'
          }
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
