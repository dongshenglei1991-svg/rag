import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// Unified API response type from backend
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// Create Axios instance
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// Request interceptor
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Add token if available
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // Handle unified response format: code !== 200 means business error
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return response
  },
  (error) => {
    // Handle HTTP errors
    let message = '网络错误，请稍后重试'

    if (error.response) {
      const status = error.response.status
      const data = error.response.data as ApiResponse | undefined

      // Use backend error message if available
      if (data?.message) {
        message = data.message
      } else {
        switch (status) {
          case 400:
            message = '请求参数错误'
            break
          case 404:
            message = '请求的资源不存在'
            break
          case 413:
            message = '文件过大'
            break
          case 500:
            message = '服务器内部错误'
            break
          case 502:
            message = '外部服务调用失败'
            break
          case 503:
            message = '服务暂时不可用'
            break
        }
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
