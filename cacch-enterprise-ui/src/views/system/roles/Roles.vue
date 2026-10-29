<template>
  <div class="p-6">
    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <!-- 筛选区 -->
      <div class="flex flex-wrap items-center gap-3">
        <el-input v-model="keyword" placeholder="角色名称 / 编码" clearable class="!w-56" @keyup.enter="fetchPage" />
        <el-button type="primary" :icon="Search" @click="fetchPage">查询</el-button>
        <el-button v-permission="'system:role:add'" type="primary" :icon="Plus" class="ml-auto" @click="openCreate">
          新增角色
        </el-button>
      </div>

      <!-- 列表 -->
      <el-table :data="rows" v-loading="loading" class="mt-4" stripe>
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="roleName" label="角色名称" width="130" />
        <el-table-column label="数据权限" width="110">
          <template #default="{ row }">{{ dataScopeText(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.builtIn === 1" type="info" size="small">内置</el-tag>
            <el-tag v-else size="small">自定义</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:role:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'system:role:grant'" link type="primary" @click="openGrant(row)">菜单授权</el-button>
            <el-button v-permission="'system:role:delete'" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="mt-4 justify-end"
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        @change="fetchPage"
      />
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增角色' : '编辑角色'" width="500px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="100px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="dialog.form.roleCode"
            :disabled="dialog.mode === 'edit' && dialog.builtIn === 1"
            placeholder="大写字母开头，如 DEPT_AUDITOR"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="dialog.form.roleName" :disabled="dialog.mode === 'edit' && dialog.builtIn === 1" />
        </el-form-item>
        <el-form-item label="数据权限" prop="dataScope">
          <el-select v-model="dialog.form.dataScope" :disabled="dialog.mode === 'edit' && dialog.roleCode === 'SUPER_ADMIN'" class="!w-full">
            <el-option v-for="s in dataScopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dialog.form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dialog.form.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 菜单授权弹窗 -->
    <el-dialog v-model="grant.visible" :title="`菜单授权 - ${grant.roleName}`" width="480px">
      <el-tree
        ref="grantTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
      />
      <template #footer>
        <el-button @click="grant.visible = false">取消</el-button>
        <el-button type="primary" :loading="grant.saving" @click="onGrant">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createRole, deleteRole, grantRoleMenus, listMenuTree, listRoleMenuIds,
  pageRoles, updateRole, type MenuTreeVO, type SysRoleVO
} from '../../../api/system'
import { formatDateTime } from '../../../utils/format'

const rows = ref<SysRoleVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const menuTree = ref<MenuTreeVO[]>([])

const dataScopes = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本部门' },
  { value: 3, label: '本部门及下级' },
  { value: 4, label: '仅本人' }
]

const formRef = ref<FormInstance>()
const dialog = reactive({
  visible: false,
  saving: false,
  mode: 'create' as 'create' | 'edit',
  editId: 0,
  builtIn: 0,
  roleCode: '',
  form: { roleCode: '', roleName: '', dataScope: 1, status: 0, remark: '' }
})

const grant = reactive({ visible: false, saving: false, roleId: 0, roleName: '' })
const grantTreeRef = ref<{
  getCheckedKeys: (leafOnly?: boolean) => number[]
  getHalfCheckedKeys: () => number[]
  setCheckedKeys: (keys: number[]) => void
}>()

const rules: FormRules = {
  roleCode: [
    { pattern: /^[A-Z][A-Z0-9_]{1,31}$/, message: '大写字母开头，仅含大写字母、数字与下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据权限档位', trigger: 'change' }]
}

function dataScopeText(scope: number): string {
  return dataScopes.find((s) => s.value === scope)?.label ?? String(scope)
}

/** 查询角色分页 */
async function fetchPage() {
  loading.value = true
  try {
    const page = await pageRoles({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    rows.value = page.list
    total.value = page.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

/** 打开新增弹窗 */
function openCreate() {
  dialog.mode = 'create'
  dialog.editId = 0
  dialog.builtIn = 0
  dialog.roleCode = ''
  dialog.form = { roleCode: '', roleName: '', dataScope: 1, status: 0, remark: '' }
  dialog.visible = true
}

/** 打开编辑弹窗 */
function openEdit(row: SysRoleVO) {
  dialog.mode = 'edit'
  dialog.editId = row.id
  dialog.builtIn = row.builtIn
  dialog.roleCode = row.roleCode
  dialog.form = {
    roleCode: row.roleCode,
    roleName: row.roleName,
    dataScope: row.dataScope,
    status: row.status,
    remark: row.remark ?? ''
  }
  dialog.visible = true
}

/** 保存角色 */
async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  dialog.saving = true
  try {
    if (dialog.mode === 'create') {
      await createRole({ ...dialog.form, roleCode: dialog.form.roleCode.trim() })
      ElMessage.success('角色创建成功')
    } else {
      await updateRole(dialog.editId, { ...dialog.form, roleCode: dialog.form.roleCode.trim() })
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

/** 删除角色 */
async function onDelete(row: SysRoleVO) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '删除确认', { type: 'error' })
  } catch {
    return
  }
  try {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    fetchPage()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

/** 打开授权弹窗（回显已授权菜单） */
async function openGrant(row: SysRoleVO) {
  grant.roleId = row.id
  grant.roleName = row.roleName
  grant.visible = true
  grant.saving = false
  try {
    if (!menuTree.value.length) {
      menuTree.value = await listMenuTree()
    }
    const checked = await listRoleMenuIds(row.id)
    // 回显全部勾选（含父节点半选态由 el-tree 自动推导）
    grantTreeRef.value?.setCheckedKeys(checked)
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

/** 提交授权（勾选 + 半选父节点一起提交） */
async function onGrant() {
  grant.saving = true
  try {
    const checked = grantTreeRef.value?.getCheckedKeys(false) ?? []
    const halfChecked = grantTreeRef.value?.getHalfCheckedKeys() ?? []
    const menuIds = Array.from(new Set([...checked, ...halfChecked]))
    await grantRoleMenus(grant.roleId, menuIds)
    ElMessage.success('授权成功，相关用户权限已刷新')
    grant.visible = false
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    grant.saving = false
  }
}

onMounted(fetchPage)
</script>
