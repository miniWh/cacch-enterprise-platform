<template>
  <div class="p-6">
    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <div class="flex items-center">
        <span class="text-[15px] font-semibold text-gray-900">部门管理</span>
        <el-button
          v-permission="'system:dept:add'"
          type="primary"
          :icon="Plus"
          class="ml-auto"
          @click="openCreate(0)"
        >
          新增部门
        </el-button>
      </div>

      <!-- 部门树表格 -->
      <el-table
        :data="treeRows"
        v-loading="loading"
        class="mt-4"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="deptName" label="部门名称" min-width="220" />
        <el-table-column prop="orderNum" label="排序" width="80" />
        <el-table-column prop="leaderId" label="负责人 ID" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="部门 ID" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:dept:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'system:dept:add'" link type="primary" @click="openCreate(row.id)">新增下级</el-button>
            <el-button v-permission="'system:dept:delete'" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增部门' : '编辑部门'" width="480px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="100px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="dialog.form.parentId"
            :data="parentOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="dialog.form.deptName" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="dialog.form.orderNum" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="负责人" prop="leaderId">
          <el-input-number v-model="dialog.form.leaderId" :min="0" placeholder="用户 ID，可空" class="!w-full" />
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
import { createDept, deleteDept, listDeptTree, updateDept, type DeptTreeVO } from '../../../api/system'

const treeRows = ref<DeptTreeVO[]>([])
const loading = ref(false)

const formRef = ref<FormInstance>()
const dialog = reactive({
  visible: false,
  saving: false,
  mode: 'create' as 'create' | 'edit',
  editId: 0,
  form: { parentId: 0, deptName: '', orderNum: 0, leaderId: undefined as number | undefined, status: 0 }
})

const rules: FormRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请填写排序号', trigger: 'blur' }]
}

/** 父部门下拉 */
const parentOptions = computed(() => {
  return [{ id: 0, deptName: '根节点', children: treeRows.value }] as unknown as DeptTreeVO[]
})

/** 拉取部门树 */
async function fetchTree() {
  loading.value = true
  try {
    treeRows.value = await listDeptTree()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

/** 打开新增弹窗 */
function openCreate(parentId: number) {
  dialog.mode = 'create'
  dialog.editId = 0
  dialog.form = { parentId, deptName: '', orderNum: 0, leaderId: undefined, status: 0 }
  dialog.visible = true
}

/** 打开编辑弹窗 */
function openEdit(row: DeptTreeVO) {
  dialog.mode = 'edit'
  dialog.editId = row.id
  dialog.form = {
    parentId: row.parentId,
    deptName: row.deptName,
    orderNum: row.orderNum,
    leaderId: row.leaderId,
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
      deptName: dialog.form.deptName,
      orderNum: dialog.form.orderNum,
      leaderId: dialog.form.leaderId || null,
      status: dialog.form.status
    }
    if (dialog.mode === 'create') {
      await createDept(payload)
      ElMessage.success('创建成功')
    } else {
      await updateDept(dialog.editId, payload)
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
async function onDelete(row: DeptTreeVO) {
  try {
    await ElMessageBox.confirm(`确认删除部门「${row.deptName}」？含下级部门或归属用户时无法删除。`, '删除确认', { type: 'error' })
  } catch {
    return
  }
  try {
    await deleteDept(row.id)
    ElMessage.success('删除成功')
    fetchTree()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

onMounted(fetchTree)
</script>
