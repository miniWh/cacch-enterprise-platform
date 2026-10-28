<template>
  <div class="p-6">
    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <div class="flex items-center">
        <span class="text-[15px] font-semibold text-gray-900">菜单管理</span>
        <span class="ml-3 text-xs text-gray-400">目录（D）/ 菜单（M）/ 按钮（B）三级结构，按钮节点维护权限标识</span>
        <el-button
          v-permission="'system:menu:add'"
          type="primary"
          :icon="Plus"
          class="ml-auto"
          @click="openCreate(0, 'D')"
        >
          新增目录
        </el-button>
      </div>

      <!-- 菜单树表格 -->
      <el-table
        :data="treeRows"
        v-loading="loading"
        class="mt-4"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="menuName" label="名称" min-width="200" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.menuType === 'D' ? 'warning' : row.menuType === 'M' ? 'success' : 'info'" size="small">
              {{ row.menuType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="70" />
        <el-table-column prop="path" label="路由地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限标识" min-width="160" show-overflow-tooltip />
        <el-table-column label="可见" width="70">
          <template #default="{ row }">{{ row.visible === 0 ? '显示' : '隐藏' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:menu:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.menuType !== 'B'"
              v-permission="'system:menu:add'"
              link
              type="primary"
              @click="openCreate(row.id, row.menuType === 'D' ? 'M' : 'B')"
            >
              新增下级
            </el-button>
            <el-button v-permission="'system:menu:delete'" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增菜单' : '编辑菜单'" width="560px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="100px">
        <el-form-item label="节点类型" prop="menuType">
          <el-radio-group v-model="dialog.form.menuType" :disabled="dialog.mode === 'edit'">
            <el-radio value="D">目录</el-radio>
            <el-radio value="M">菜单</el-radio>
            <el-radio value="B">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="menuName">
          <el-input v-model="dialog.form.menuName" />
        </el-form-item>
        <el-form-item label="父节点" prop="parentId">
          <el-tree-select
            v-model="dialog.form.parentId"
            :data="parentOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="dialog.form.orderNum" :min="0" :max="999" />
        </el-form-item>
        <template v-if="dialog.form.menuType !== 'B'">
          <el-form-item label="路由地址" prop="path">
            <el-input v-model="dialog.form.path" placeholder="如 /system/users" />
          </el-form-item>
          <el-form-item v-if="dialog.form.menuType === 'M'" label="组件路径" prop="component">
            <el-input v-model="dialog.form.component" placeholder="如 system/users/Users" />
          </el-form-item>
          <el-form-item label="图标" prop="icon">
            <el-input v-model="dialog.form.icon" placeholder="可空" />
          </el-form-item>
          <el-form-item label="可见性" prop="visible">
            <el-radio-group v-model="dialog.form.visible">
              <el-radio :value="0">显示</el-radio>
              <el-radio :value="1">隐藏</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
        <el-form-item v-if="dialog.form.menuType === 'B'" label="权限标识" prop="perms">
          <el-input v-model="dialog.form.perms" placeholder="如 system:user:add，全局唯一" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dialog.form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createMenu, deleteMenu, listMenuTree, updateMenu, type MenuTreeVO } from '../../../api/system'

const treeRows = ref<MenuTreeVO[]>([])
const loading = ref(false)

const formRef = ref<FormInstance>()
const dialog = reactive({
  visible: false,
  saving: false,
  mode: 'create' as 'create' | 'edit',
  editId: 0,
  form: {
    parentId: 0,
    menuName: '',
    menuType: 'D' as 'D' | 'M' | 'B',
    orderNum: 0,
    path: '',
    component: '',
    perms: '',
    icon: '',
    visible: 0,
    status: 0
  }
})

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
  orderNum: [{ required: true, message: '请填写排序号', trigger: 'blur' }]
}

/** 父节点下拉（目录/菜单可选，新增按钮时父节点可为菜单或目录） */
const parentOptions = computed(() => {
  const root = [{ id: 0, menuName: '根节点', children: treeRows.value }]
  return root as unknown as MenuTreeVO[]
})

/** 拉取菜单树 */
async function fetchTree() {
  loading.value = true
  try {
    treeRows.value = await listMenuTree()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

/** 打开新增弹窗 */
function openCreate(parentId: number, menuType: 'D' | 'M' | 'B') {
  dialog.mode = 'create'
  dialog.editId = 0
  dialog.form = {
    parentId,
    menuName: '',
    menuType,
    orderNum: 0,
    path: '',
    component: '',
    perms: '',
    icon: '',
    visible: 0,
    status: 0
  }
  dialog.visible = true
}

/** 打开编辑弹窗 */
function openEdit(row: MenuTreeVO) {
  dialog.mode = 'edit'
  dialog.editId = row.id
  dialog.form = {
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    orderNum: row.orderNum,
    path: row.path ?? '',
    component: row.component ?? '',
    perms: row.perms ?? '',
    icon: row.icon ?? '',
    visible: row.visible,
    status: row.status
  }
  dialog.visible = true
}

/** 保存 */
async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  dialog.saving = true
  try {
    const payload = {
      parentId: dialog.form.parentId,
      menuName: dialog.form.menuName,
      menuType: dialog.form.menuType,
      orderNum: dialog.form.orderNum,
      path: dialog.form.menuType === 'B' ? null : dialog.form.path || null,
      component: dialog.form.menuType === 'M' ? dialog.form.component || null : null,
      perms: dialog.form.menuType === 'B' ? dialog.form.perms || null : null,
      icon: dialog.form.menuType === 'B' ? null : dialog.form.icon || null,
      visible: dialog.form.visible,
      status: dialog.form.status
    }
    if (dialog.mode === 'create') {
      await createMenu(payload)
      ElMessage.success('创建成功')
    } else {
      await updateMenu(dialog.editId, payload)
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    fetchTree()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    dialog.saving = false
  }
}

/** 删除 */
async function onDelete(row: MenuTreeVO) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.menuName}」？已授权或含子节点的菜单无法删除。`, '删除确认', { type: 'error' })
  } catch {
    return
  }
  try {
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    fetchTree()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

onMounted(fetchTree)
</script>
