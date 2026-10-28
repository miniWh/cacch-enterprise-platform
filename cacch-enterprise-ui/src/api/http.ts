import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { NOT_LOGIN_CODE, REFRESH_INVALID_CODE, SUCCESS_CODE, type ApiResult } from './codes'

/**
 * axios 实例：自动携带 access token，业务失败自动抛错，401 触发静默刷新重放
 */
export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/** 静默刷新单例锁与等待队列（并发 401 时只发起一次刷新） */
let refreshing: Promise<boolean> | null = null

/** 无需携带令牌的接口前缀（登录前场景） */
const WHITELIST = ['/auth/captcha', '/auth/login', '/auth/refresh']

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token && config.url && !WHITELIST.some((p) => config.url!.startsWith(p))) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 尝试用 refresh token 静默换取新令牌对
 *
 * @return true 表示刷新成功（新令牌已写入 localStorage）
 */
async function tryRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem('refresh_token')
  if (!refreshToken) {
    return false
  }
  try {
    const resp = await axios.post<ApiResult<{ accessToken: string; refreshToken: string }>>(
      '/api/auth/refresh',
      { refreshToken },
      { timeout: 10000 }
    )
    if (resp.data.code === SUCCESS_CODE && resp.data.data) {
      localStorage.setItem('access_token', resp.data.data.accessToken)
      localStorage.setItem('refresh_token', resp.data.data.refreshToken)
      return true
    }
    return false
  } catch {
    return false
  }
}

http.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    if (response.data.code !== SUCCESS_CODE) {
      return Promise.reject(new Error(response.data.message || '请求失败'))
    }
    return response
  },
  async (error: AxiosError<ApiResult>) => {
    const config = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
    const code = error.response?.data?.code
    // 401 未登录且尚未重试过：尝试静默刷新后重放原请求
    if (code === NOT_LOGIN_CODE && config && !config._retried && config.url && !config.url.includes('/auth/')) {
      config._retried = true
      refreshing = refreshing ?? tryRefresh()
      const ok = await refreshing
      refreshing = null
      if (ok) {
        return http.request(config)
      }
      clearTokens()
      window.location.hash = '#/login'
      window.location.reload()
      return Promise.reject(error)
    }
    // 刷新令牌失效或被互踢：清空令牌跳登录
    if (code === REFRESH_INVALID_CODE || code === NOT_LOGIN_CODE || code === undefined) {
      if (config?.url?.startsWith('/auth/')) {
        clearTokens()
        window.location.hash = '#/login'
      }
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '网络异常'))
  }
)

/** 清空本地令牌 */
export function clearTokens() {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
}
