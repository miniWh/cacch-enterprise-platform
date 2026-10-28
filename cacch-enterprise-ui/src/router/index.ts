import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/**
 * 组件路径 → 动态导入映射（对应 sys_menu.component 字段值）
 */
const viewModules = import.meta.glob('../views/**/*.vue')

function resolveComponent(component?: string) {
  if (!component) {
    return undefined
  }
  const matcher = viewModules[`../views/${component}.vue`]
  if (!matcher) {
    console.warn(`菜单组件不存在: ${component}`)
    return undefined
  }
  return matcher
}

/** 静态路由：登录页与布局骨架（动态菜单路由挂载在 Layout 下） */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('../layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: []
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

/** 是否已完成动态路由注册 */
let dynamicAdded = false

/**
 * 将后端菜单树（目录 D + 菜单 M）转换为路由记录并挂载到 Layout
 */
export function registerDynamicRoutes() {
  if (dynamicAdded) return
  const auth = useAuthStore()
  const layoutRoute = router.getRoutes().find((r) => r.path === '/')
  if (!layoutRoute) return

  const walk = (nodes: typeof auth.menus) => {
    for (const menu of nodes) {
      if (menu.menuType === 'D') {
        walk(menu.children ?? [])
        continue
      }
      const component = resolveComponent(menu.component)
      if (!component) continue
      router.addRoute('/', {
        path: menu.path,
        name: `menu-${menu.id}`,
        component,
        meta: { title: menu.menuName, menuId: menu.id }
      })
    }
  }
  walk(auth.menus)
  dynamicAdded = true
}

/** 重置动态路由（登出后调用，防止残留） */
export function resetDynamicRoutes() {
  dynamicAdded = false
}

/**
 * 全局守卫：无令牌跳登录；有令牌先加载用户信息再注册动态路由
 */
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const hasToken = !!localStorage.getItem('access_token')

  if (to.meta.public) {
    return hasToken ? { path: '/dashboard' } : true
  }
  if (!hasToken) {
    return { path: '/login', query: to.fullPath === '/' ? undefined : { redirect: to.fullPath } }
  }
  try {
    await auth.loadProfile()
    registerDynamicRoutes()
    return true
  } catch {
    auth.doLogout()
    resetDynamicRoutes()
    return { path: '/login' }
  }
})

export default router
