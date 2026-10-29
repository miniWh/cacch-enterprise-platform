<template>
  <div class="p-6">
    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <!-- 筛选区 -->
      <div class="flex flex-wrap items-center gap-3">
        <el-input v-model="query.username" placeholder="工号" clearable class="!w-40" @keyup.enter="fetchPage" />
        <el-input v-model="query.realName" placeholder="姓名" clearable class="!w-40" @keyup.enter="fetchPage" />
        <el-input v-model="query.phone" placeholder="手机号" clearable class="!w-40" @keyup.enter="fetchPage" />
        <el-tree-select
          v-model="query.deptId"
          :data="deptOptions"
          :props="{ label: 'deptName', value: 'id', children: 'children' }"
          node-key="id"
          check-strictly
          clearable
          placeholder="归属部门"
          class="!w-48"
        />
        <el-select v-model="query.status" clearable placeholder="状态" class="!w-28">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchPage">查询</el-button>
        <div class="ml-auto flex gap-2">
          <el-button v-permission="'system:user:add'" type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
          <el-button v-permission="'system:user:offline'" :icon="VideoPlay" @click="fetchOnline">在线用户</el-button>
        </div>
      </div>

      <!-- 列表 -->
      <el-table :data="rows" v-loading="loading" class="mt-4" stripe>
        <el-table-column prop="username" label="工号" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:user:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'system:user:status'" link :type="row.status === 0 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 0 ? '停用' : '启用' }}
            </el-button>
            <el-button v-permission="'system:user:resetPwd'" link type="primary" @click="openResetPwd(row)">重置密码</el-button>
            <el-dropdown v-permission="'system:user:delete'" trigger="click" @command="(cmd: string) => onRowCommand(cmd, row)">
              <el-button link type="primary" class="ml-2">更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="offline" v-if="row.status === 0">强制下线</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="mt-4 justify-end"
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        @change="fetchPage"
      />
    </div>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增用户' : '编辑用户'" width="520px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
        <el-form-item label="工号" prop="username">
          <el-input v-model="dialog.form.username" :disabled="dialog.mode === 'edit'" placeholder="创建后不可修改" />
        </el-form-item>
        <el-form-item v-if="dialog.mode === 'create'" label="初始密码" prop="password">
          <el-input v-model="dialog.form.password" type="password" show-password placeholder="8~64 位，含字母与数字" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="dialog.form.realName" />
        </el-form-item>
        <el-form-item label="归属部门" prop="deptId">
          <el-tree-select
            v-model="dialog.form.deptId"
            :data="deptOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="dialog.form.phone" placeholder="可空" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="dialog.form.email" placeholder="可空" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="dialog.form.roleIds" multiple collapse-tags class="!w-full" placeholder="可多选">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwd.visible" title="重置密码" width="440px">
      <p class="mb-3 text-[13px] text-gray-500">为 <b>{{ resetPwd.realName }}</b>（{{ resetPwd.username }}）设置新密码，重置后其全部会话将被清除。</p>
      <el-input v-model="resetPwd.newPassword" type="password" show-password placeholder="8~64 位，须同时包含字母与数字" />
      <template #footer>
        <el-button @click="resetPwd.visible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwd.saving" @click="onResetPwd">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 在线用户弹窗 -->
    <el-dialog v-model="online.visible" title="在线用户" width="640px">
      <el-table :data="online.rows" v-loading="online.loading" stripe>
        <el-table-column prop="username" label="工号" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="登录时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column prop="loginIp" label="登录 IP" min-width="130" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowDown, Plus, Search, VideoPlay } from '@element-plus/icons-vue'
import {
  createUser, deleteUser, getUserDetail, listDeptTree, listEnabledRoles, listOnlineUsers,
  offlineUser, pageUsers, resetUserPassword, updateUser, updateUserStatus,
  type DeptTreeVO, type OnlineUserVO, type SysRoleSimpleVO, type SysUserVO
} from '../../../api/system'
import { formatDateTime } from '../../../utils/format'

const rows = ref<SysUserVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const deptOptions = ref<DeptTreeVO[]>([])
const roles = ref<SysRoleSimpleVO[]>([])

const query = reactive({ username: '', realName: '', phone: '', deptId: undefined as number | undefined, status: undefined as number | undefined })

const formRef = ref<FormInstance>()
const dialog = reactive({
  visible: false,
  saving: false,
  mode: 'create' as 'create' | 'edit',
  editId: 0,
  form: { username: '', password: '', realName: '', deptId: undefined as number | undefined, phone: '', email: '', roleIds: [] as number[] }
})

const resetPwd = reactive({ visible: false, saving: false, userId: 0, username: '', realName: '', newPassword: '' })
const online = reactive({ visible: false, loading: false, rows: [] as OnlineUserVO[] })

const rules: FormRules = {
  username: [{ required: true, message: '请输入工号', trigger: 'blur' }, { pattern: /^[A-Za-z0-9_]{2,32}$/, message: '2~32 位字母、数字或下划线', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }, { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '8~64 位且须同时包含字母与数字', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择归属部门', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

/** 查询用户分页 */
async function fetchPage() {
  loading.value = true
  try {
    const page = await pageUsers({ pageNum: pageNum.value, pageSize: pageSize.value, ...query })
    rows.value = page.list
    total.value = page.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

/** 查询部门树与可选角色（表单下拉数据源） */
async function fetchOptions() {
  try {
    const [depts, enabledRoles] = await Promise.all([listDeptTree(), listEnabledRoles()])
    deptOptions.value = depts
    roles.value = enabledRoles
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

/** 打开新增弹窗 */
function openCreate() {
  dialog.mode = 'create'
  dialog.editId = 0
  dialog.form = { username: '', password: '', realName: '', deptId: undefined, phone: '', email: '', roleIds: [] }
  dialog.visible = true
}

/** 打开编辑弹窗（回显角色） */
async function openEdit(row: SysUserVO) {
  try {
    const detail = await getUserDetail(row.id)
    dialog.mode = 'edit'
    dialog.editId = row.id
    dialog.form = {
      username: detail.username,
      password: '',
      realName: detail.realName,
      deptId: detail.deptId,
      phone: detail.phone ?? '',
      email: detail.email ?? '',
      roleIds: detail.roleIds ?? []
    }
    dialog.visible = true
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

/** 保存新增/编辑 */
async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  dialog.saving = true
  try {
    if (dialog.mode === 'create') {
      const { username, password, realName, deptId, phone, email, roleIds } = dialog.form
      const resp = await createUser({ username, password, realName, deptId: deptId!, phone: phone || undefined, email: email || undefined, roleIds })
      ElMessage.success(`用户创建成功（ID: ${resp.data}）`)
    } else {
      const { realName, deptId, phone, email, roleIds } = dialog.form
      await updateUser(dialog.editId, { realName, deptId: deptId!, phone: phone || undefined, email: email || undefined, roleIds })
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    fetchPage()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    dialog.saving = false
  }
}

/** 启用/停用 */
async function toggleStatus(row: SysUserVO) {
  const target = row.status === 0 ? 1 : 0
  const action = target === 1 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户「${row.realName}」？${target === 1 ? '停用后其会话将被清除。' : ''}`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateUserStatus(row.id, target)
    ElMessage.success(`${action}成功`)
    fetchPage()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

/** 打开重置密码弹窗 */
function openResetPwd(row: SysUserVO) {
  resetPwd.userId = row.id
  resetPwd.username = row.username
  resetPwd.realName = row.realName
  resetPwd.newPassword = ''
  resetPwd.visible = true
}

/** 提交重置密码 */
async function onResetPwd() {
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,64}$/.test(resetPwd.newPassword)) {
    ElMessage.warning('新密码须为 8~64 位且同时包含字母与数字')
    return
  }
  resetPwd.saving = true
  try {
    await resetUserPassword(resetPwd.userId, resetPwd.newPassword)
    ElMessage.success('密码已重置，目标用户需重新登录')
    resetPwd.visible = false
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    resetPwd.saving = false
  }
}

/** 行更多操作 */
async function onRowCommand(command: string, row: SysUserVO) {
  if (command === 'offline') {
    try {
      await ElMessageBox.confirm(`确认强制下线「${row.realName}」？`, '提示', { type: 'warning' })
    } catch {
      return
    }
    try {
      await offlineUser(row.id)
      ElMessage.success('已强制下线')
    } catch (e) {
      ElMessage.error((e as Error).message)
    }
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`确认删除用户「${row.realName}」？删除后不可恢复。`, '删除确认', { type: 'error' })
    } catch {
      return
    }
    try {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      fetchPage()
    } catch (e) {
      ElMessage.error((e as Error).message)
    }
  }
}

/** 打开在线用户弹窗 */
async function fetchOnline() {
  online.visible = true
  online.loading = true
  try {
    online.rows = await listOnlineUsers()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    online.loading = false
  }
}

onMounted(() => {
  fetchPage()
  fetchOptions()
})
</script>
