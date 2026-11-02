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
    children: [
      // 个人中心：平台级固定页，静态注册（不依赖菜单数据，隐藏菜单后深链仍可直达）
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/profile/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
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
      // 隐藏菜单不注册路由（visible=1 仅保留数据用于权限收集与菜单管理）
      if (menu.visible === 1) continue
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
 * 登录后的落地页：取用户菜单树中第一个可注册的菜单路径。
 *
 * 不能硬编码 '/dashboard'：用户若未被分配该菜单，该路径并未注册，
 * 命中兜底路由 redirect 回 /login，守卫又返回 /dashboard，
 * 形成 redirect 自环 → 栈溢出 → 页面彻底卡死（表现为"登录请求超时"，
 * 清缓存/强刷均无效，因为与缓存无关）。
 *
 * 用户无任何可用菜单时落到 '/profile'：它是静态注册的固定页，必定可解析。
 */
export function defaultLandingPath(): string {
  const auth = useAuthStore()
  const walk = (nodes: typeof auth.menus): string | null => {
    for (const menu of nodes) {
      if (menu.visible === 1) continue
      if (menu.menuType === 'D') {
        const sub = walk(menu.children ?? [])
        if (sub) return sub
        continue
      }
      if (menu.path && resolveComponent(menu.component)) return menu.path
    }
    return null
  }
  return walk(auth.menus) ?? '/profile'
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
  } catch (e) {
    console.error('【路由】用户信息加载失败，已清除登录态并跳转登录页', e)
    auth.doLogout()
    resetDynamicRoutes()
    return { path: '/login' }
  }

  // 注册完成后重定向到该用户实际拥有的第一个菜单页
  // （硬编码 /dashboard 会让未分配该菜单的用户陷入 redirect 自环）
  if (to.path === '/' || to.path === '/login') {
    return { path: defaultLandingPath() }
  }
  return true
})

export default router
