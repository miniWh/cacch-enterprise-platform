import type { Directive } from 'vue'
import { useAuthStore } from '../stores/auth'

/**
 * v-permission 按钮权限指令：无权限时移除元素
 *
 * 用法：v-permission="'system:user:add'" 或 v-permission="['system:user:add', 'system:user:edit']"
 */
export const permission: Directive<HTMLElement, string | string[] | undefined> = {
  mounted(el, binding) {
    const auth = useAuthStore()
    const required = binding.value
    if (!required) return
    const list = Array.isArray(required) ? required : [required]
    if (!list.some((p) => auth.hasPerm(p))) {
      el.parentNode?.removeChild(el)
    }
  }
}
