import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// 请求队列（用于处理 token 刷新时的请求）
let requestQueue: (() => void)[] = []
let isRefreshing = false

// 获取 Token
function getToken(): string | null {
  return localStorage.getItem('token')
}

// 获取 refreshToken
function getRefreshToken(): string | null {
  return localStorage.getItem('refreshToken')
}

// 保存 Token
function setToken(token: string, refreshToken?: string): void {
  localStorage.setItem('token', token)
  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken)
  }
}

// 清除 Token
function clearToken(): void {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
}

// 刷新 Token
async function refreshToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return null

  try {
    const response = await axios.post('/api/auth/refresh', { refreshToken })
    const { token, refreshToken: newRefreshToken } = response.data
    setToken(token, newRefreshToken)
    return token
  } catch {
    clearToken()
    return null
  }
}

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加 Token
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  async (response: AxiosResponse) => {
    const { data, config } = response

    // 处理业务错误码
    if (data.code !== undefined && data.code !== 200 && data.code !== 0) {
      // 业务逻辑错误
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }

    return data
  },
  async (error) => {
    const { response, config } = error

    // 无响应处理
    if (!response) {
      ElMessage.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }

    const originalRequest = config as InternalAxiosRequestConfig & { _retry?: boolean }

    switch (response.status) {
      case 401:
        // Token 过期处理
        if (!originalRequest._retry) {
          if (isRefreshing) {
            // 等待 token 刷新完成
            return new Promise((resolve) => {
              requestQueue.push(() => {
                resolve(service(originalRequest))
              })
            })
          }

          originalRequest._retry = true
          isRefreshing = true

          try {
            const newToken = await refreshToken()
            if (newToken) {
              // Token 刷新成功，重试所有排队的请求
              requestQueue.forEach(cb => cb())
              requestQueue = []
              // 重试当前请求
              if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${newToken}`
              }
              return service(originalRequest)
            }
          } catch {
            // 刷新失败
          } finally {
            isRefreshing = false
          }
        }

        // 无法刷新 Token，提示并跳转登录
        clearToken()
        ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          router.push('/login')
        }).catch(() => {})
        return Promise.reject(error)

      case 403:
        ElMessage.error('没有权限访问该资源')
        return Promise.reject(error)

      case 404:
        ElMessage.error('请求的资源不存在')
        return Promise.reject(error)

      case 500:
        ElMessage.error('服务器内部错误，请稍后重试')
        return Promise.reject(error)

      case 502:
        ElMessage.error('网关错误，请稍后重试')
        return Promise.reject(error)

      case 503:
        ElMessage.error('服务不可用，请稍后重试')
        return Promise.reject(error)

      case 504:
        ElMessage.error('网关超时，请稍后重试')
        return Promise.reject(error)

      default:
        ElMessage.error(response.data?.message || `请求失败 (${response.status})`)
        return Promise.reject(error)
    }
  }
)

// 导出封装的请求方法
export const request = {
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, { params, ...config })
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, { params, ...config })
  },

  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.patch(url, data, config)
  },

  // 文件上传（上传进度）
  upload<T = any>(
    url: string,
    file: File | FormData,
    onProgress?: (percent: number) => void
  ): Promise<T> {
    const formData = file instanceof File ? new FormData() : file
    if (file instanceof File) {
      formData.append('file', file)
    }

    return service.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded / progressEvent.total) * 100)
          onProgress(percent)
        }
      }
    })
  },

  // 下载文件
  download(url: string, params?: any, filename?: string): Promise<void> {
    return service
      .get(url, {
        params,
        responseType: 'blob',
        timeout: 60000
      })
      .then((response: AxiosResponse) => {
        const blob = new Blob([response.data])
        const downloadElement = document.createElement('a')
        const href = window.URL.createObjectURL(blob)
        downloadElement.href = href
        downloadElement.download = filename || '下载文件'
        document.body.appendChild(downloadElement)
        downloadElement.click()
        document.body.removeChild(downloadElement)
        window.URL.revokeObjectURL(href)
      })
  }
}

// 导出 service 实例和工具函数
export { service, getToken, setToken, clearToken }

export default service
