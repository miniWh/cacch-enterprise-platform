<template>
  <div class="p-6">
    <div class="mx-auto max-w-[720px]">
      <!-- 用户资料卡片 -->
      <div class="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
        <h2 class="text-[15px] font-semibold text-gray-900">个人资料</h2>
        <div class="mt-4 flex items-center gap-4">
          <span class="flex h-14 w-14 items-center justify-center rounded-full bg-indigo-100 text-lg font-medium text-indigo-600">
            {{ avatarChar }}
          </span>
          <div>
            <p class="text-base font-medium text-gray-900">{{ auth.profile?.realName ?? '-' }}</p>
            <p class="mt-0.5 text-[13px] text-gray-400">{{ auth.profile?.username ?? '' }}</p>
          </div>
        </div>
        <el-descriptions :column="2" border class="mt-5">
          <el-descriptions-item label="工号">{{ auth.profile?.username ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ auth.profile?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ auth.profile?.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="auth.profile?.status === 0 ? 'success' : 'danger'" size="small">
              {{ auth.profile?.status === 0 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 修改密码 -->
      <div class="mt-4 rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
        <h2 class="text-[15px] font-semibold text-gray-900">修改密码</h2>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="mt-4 max-w-[420px]">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="form.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="8~64 位，须同时包含字母与数字" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="onSubmit">确认修改</el-button>
          </el-form-item>
        </el-form>
        <p class="text-xs text-gray-400">修改成功后将清除全部登录会话，需使用新密码重新登录。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '8~64 位且须同时包含字母与数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/** 头像字符（姓名末字） */
const avatarChar = computed(() => {
  const name = auth.profile?.realName
  return name ? name.slice(-1) : '-'
})

/** 提交修改密码 */
async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await auth.changePwd(form.oldPassword, form.newPassword)
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    router.replace('/login')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    saving.value = false
  }
}
</script>
