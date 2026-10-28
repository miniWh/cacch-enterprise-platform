/**
 * 后端统一返回体结构
 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/** 业务成功码 */
export const SUCCESS_CODE = 0

/** 未登录/无令牌业务码 */
export const NOT_LOGIN_CODE = 40100

/** 令牌无效（签名错误/格式错误）业务码 */
export const TOKEN_INVALID_CODE = 40104

/** 被互踢/强制下线业务码 */
export const KICKED_OUT_CODE = 40105

/** 刷新令牌无效或已过期业务码 */
export const REFRESH_INVALID_CODE = 40108
