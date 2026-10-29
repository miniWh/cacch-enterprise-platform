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
    name: 'layout',
    component: () => import('../layout/BasicLayout.vue'),
    children: []
  },
  // 兜底不得 redirect 到动态路由：matcher 层解析先于守卫执行，
  // 动态路由未注册时会形成 redirect 自环导致栈溢出；/login 为静态路由，安全
  { path: '/:pathMatch(.*)*', redirect: '/login' }
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
      // 第一个参数是父路由 name（而非 path），根路由须声明 name: 'layout' 方可挂载子路由
      router.addRoute('layout', {
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
 * 全局守卫：无令牌放行公开页/跳登录；有令牌一律先加载用户信息并注册动态路由，
 * 再放行或将登录页重定向到首页（否则登录页刷新会先于路由注册重定向 /dashboard，
 * 未注册的路径命中兜底路由形成无限 redirect 导致栈溢出）
 */
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const hasToken = !!localStorage.getItem('access_token')

  if (!hasToken) {
    if (to.meta.public) {
      return true
    }
    return { path: '/login', query: to.fullPath === '/' ? undefined : { redirect: to.fullPath } }
  }

  try {
    await auth.loadProfile()
    registerDynamicRoutes()
  } catch {
    auth.doLogout()
    resetDynamicRoutes()
    return { path: '/login' }
  }

  // 注册完成后统一重定向到首页（此时 /dashboard 已注册，matcher 可正常解析）
  if (to.path === '/' || to.path === '/login') {
    return { path: '/dashboard' }
  }
  return true
})

export default router
