import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { changePassword, getProfile, login as loginApi, logout as logoutApi, type MenuVO, type UserProfileVO } from '../api/auth'
import { clearTokens } from '../api/http'

/**
 * 认证状态：双令牌、当前用户信息（菜单树 + 权限标识）
 */
export const useAuthStore = defineStore('auth', () => {
  const profile = ref<UserProfileVO | null>(null)
  const menus = computed<MenuVO[]>(() => profile.value?.menus ?? [])

  /** 是否已加载用户信息 */
  const loaded = ref(false)

  /**
   * 账号密码登录并保存令牌
   */
  async function login(form: { account: string; password: string; captchaId: string; captchaCode: string }) {
    const tokens = await loginApi(form)
    localStorage.setItem('access_token', tokens.accessToken)
    localStorage.setItem('refresh_token', tokens.refreshToken)
    profile.value = null
    loaded.value = false
  }

  /**
   * 加载当前用户信息（守卫与布局刷新时调用，已加载则跳过）
   */
  async function loadProfile(force = false) {
    if (loaded.value && !force) {
      return profile.value
    }
    profile.value = await getProfile()
    loaded.value = true
    return profile.value
  }

  /** 修改密码（成功后清除全部会话，跳转登录） */
  async function changePwd(oldPassword: string, newPassword: string) {
    await changePassword({ oldPassword, newPassword })
    doLogout()
  }

  /** 登出并清空本地状态 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      doLogout()
    }
  }

  /** 清空本地令牌与用户信息 */
  function doLogout() {
    clearTokens()
    profile.value = null
    loaded.value = false
  }

  /** 是否拥有指定权限标识（超管*或集合包含） */
  function hasPerm(perm?: string): boolean {
    if (!perm) return true
    const perms = profile.value?.perms ?? []
    return perms.includes('*') || perms.includes(perm)
  }

  return { profile, menus, loaded, login, loadProfile, changePwd, logout, doLogout, hasPerm }
})
