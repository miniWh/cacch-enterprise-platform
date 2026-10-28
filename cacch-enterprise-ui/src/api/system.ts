import { http } from './http'
import type { ApiResult } from './codes'

/** 分页返回结构 */
export interface PageResult<T> {
  total: number
  list: T[]
}

/** 用户视图对象 */
export interface SysUserVO {
  id: number
  username: string
  phone?: string
  realName: string
  deptId?: number
  email?: string
  status: number
  source: string
  roleIds?: number[]
  lastLoginTime?: string
  createTime: string
}

/** 角色视图对象 */
export interface SysRoleVO {
  id: number
  roleCode: string
  roleName: string
  dataScope: number
  builtIn: number
  status: number
  remark?: string
  createTime: string
}

/** 角色精简视图对象 */
export interface SysRoleSimpleVO {
  id: number
  roleCode: string
  roleName: string
}

/** 菜单管理树节点 */
export interface MenuTreeVO {
  id: number
  parentId: number
  menuName: string
  menuType: 'D' | 'M' | 'B'
  orderNum: number
  path?: string
  component?: string
  perms?: string
  icon?: string
  visible: number
  status: number
  children: MenuTreeVO[]
}

/** 部门树节点 */
export interface DeptTreeVO {
  id: number
  parentId: number
  ancestors: string
  deptName: string
  orderNum: number
  leaderId?: number
  status: number
  children: DeptTreeVO[]
}

/** 在线用户 */
export interface OnlineUserVO {
  userId: number
  username: string
  realName: string
  deptId?: number
  loginTime?: string
  loginIp?: string
}

// ---------- 用户管理 ----------

export function pageUsers(params: {
  pageNum?: number
  pageSize?: number
  username?: string
  realName?: string
  phone?: string
  deptId?: number
  status?: number
}) {
  return http.get<ApiResult<PageResult<SysUserVO>>>('/v1/system/users', { params }).then((r) => r.data.data)
}

export function getUserDetail(id: number) {
  return http.get<ApiResult<SysUserVO>>(`/v1/system/users/${id}`).then((r) => r.data.data)
}

export function createUser(data: {
  username: string
  password: string
  realName: string
  deptId: number
  phone?: string
  email?: string
  roleIds?: number[]
}) {
  return http.post<ApiResult<number>>('/v1/system/users', data).then((r) => r.data)
}

export function updateUser(id: number, data: {
  realName?: string
  deptId?: number
  phone?: string
  email?: string
  roleIds?: number[] | null
}) {
  return http.put<ApiResult<void>>(`/v1/system/users/${id}`, data).then((r) => r.data)
}

export function updateUserStatus(id: number, status: number) {
  return http.put<ApiResult<void>>(`/v1/system/users/${id}/status`, { status }).then((r) => r.data)
}

export function resetUserPassword(id: number, newPassword: string) {
  return http.put<ApiResult<void>>(`/v1/system/users/${id}/password`, { newPassword }).then((r) => r.data)
}

export function offlineUser(id: number) {
  return http.put<ApiResult<void>>(`/v1/system/users/${id}/offline`).then((r) => r.data)
}

export function deleteUser(id: number) {
  return http.delete<ApiResult<void>>(`/v1/system/users/${id}`).then((r) => r.data)
}

export function listOnlineUsers() {
  return http.get<ApiResult<OnlineUserVO[]>>('/v1/system/users/online').then((r) => r.data.data)
}

// ---------- 角色管理 ----------

export function pageRoles(params: { pageNum?: number; pageSize?: number; keyword?: string }) {
  return http.get<ApiResult<PageResult<SysRoleVO>>>('/v1/system/roles', { params }).then((r) => r.data.data)
}

export function listEnabledRoles() {
  return http.get<ApiResult<SysRoleSimpleVO[]>>('/v1/system/roles/enabled').then((r) => r.data.data)
}

export function listRoleMenuIds(id: number) {
  return http.get<ApiResult<number[]>>(`/v1/system/roles/${id}/menus`).then((r) => r.data.data)
}

export function createRole(data: { roleCode: string; roleName: string; dataScope: number; status: number; remark?: string }) {
  return http.post<ApiResult<number>>('/v1/system/roles', data).then((r) => r.data)
}

export function updateRole(id: number, data: { roleCode?: string; roleName: string; dataScope: number; status: number; remark?: string }) {
  return http.put<ApiResult<void>>(`/v1/system/roles/${id}`, data).then((r) => r.data)
}

export function deleteRole(id: number) {
  return http.delete<ApiResult<void>>(`/v1/system/roles/${id}`).then((r) => r.data)
}

export function grantRoleMenus(id: number, menuIds: number[]) {
  return http.put<ApiResult<void>>(`/v1/system/roles/${id}/menus`, { menuIds }).then((r) => r.data)
}

// ---------- 菜单管理 ----------

export function listMenuTree() {
  return http.get<ApiResult<MenuTreeVO[]>>('/v1/system/menus/tree').then((r) => r.data.data)
}

export function createMenu(data: Record<string, unknown>) {
  return http.post<ApiResult<void>>('/v1/system/menus', data).then((r) => r.data)
}

export function updateMenu(id: number, data: Record<string, unknown>) {
  return http.put<ApiResult<void>>(`/v1/system/menus/${id}`, data).then((r) => r.data)
}

export function deleteMenu(id: number) {
  return http.delete<ApiResult<void>>(`/v1/system/menus/${id}`).then((r) => r.data)
}

// ---------- 部门管理 ----------

export function listDeptTree() {
  return http.get<ApiResult<DeptTreeVO[]>>('/v1/system/depts/tree').then((r) => r.data.data)
}

export function createDept(data: Record<string, unknown>) {
  return http.post<ApiResult<void>>('/v1/system/depts', data).then((r) => r.data)
}

export function updateDept(id: number, data: Record<string, unknown>) {
  return http.put<ApiResult<void>>(`/v1/system/depts/${id}`, data).then((r) => r.data)
}

export function deleteDept(id: number) {
  return http.delete<ApiResult<void>>(`/v1/system/depts/${id}`).then((r) => r.data)
}
