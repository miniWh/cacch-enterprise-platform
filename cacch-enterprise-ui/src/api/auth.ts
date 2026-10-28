import { http } from './http'
import type { ApiResult } from './codes'

/** 验证码视图对象 */
export interface CaptchaVO {
  captchaId: string
  imageBase64: string
}

/** 双令牌视图对象 */
export interface LoginTokenVO {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  tokenType: string
}

/** 菜单树节点视图对象 */
export interface MenuVO {
  id: number
  parentId: number
  menuName: string
  menuType: 'D' | 'M'
  path?: string
  component?: string
  icon?: string
  visible: number
  orderNum: number
  children: MenuVO[]
}

/** 当前登录用户信息视图对象 */
export interface UserProfileVO {
  userId: number
  username: string
  phone?: string
  realName: string
  deptId?: number
  email?: string
  status: number
  menus: MenuVO[]
  perms: string[]
}

/** 获取图形验证码 */
export function getCaptcha() {
  return http.get<ApiResult<CaptchaVO>>('/auth/captcha').then((r) => r.data.data)
}

/** 账号密码登录 */
export function login(data: { account: string; password: string; captchaId: string; captchaCode: string }) {
  return http.post<ApiResult<LoginTokenVO>>('/auth/login', data).then((r) => r.data.data)
}

/** 登出 */
export function logout() {
  return http.post<ApiResult<void>>('/auth/logout').then((r) => r.data)
}

/** 查询当前登录用户信息 */
export function getProfile() {
  return http.get<ApiResult<UserProfileVO>>('/auth/profile').then((r) => r.data.data)
}

/** 修改密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return http.put<ApiResult<void>>('/auth/profile/password', data).then((r) => r.data)
}
