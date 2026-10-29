<template>
  <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-violet-50">
    <div class="w-[400px] rounded-2xl border border-gray-100 bg-white p-8 shadow-lg">
      <div class="flex flex-col items-center">
        <img src="../../assets/logo-cac-group.png" alt="泰禾集团" class="h-12 w-auto" />
        <h1 class="mt-4 text-xl font-semibold text-gray-900">Cacch 企业管理平台</h1>
        <p class="mt-1 text-[13px] text-gray-400">统一身份认证 · 账号密码登录</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" class="mt-8" @keyup.enter="onSubmit">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="工号 / 手机号" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="flex w-full gap-2">
            <el-input v-model="form.captchaCode" placeholder="验证码" :prefix-icon="Key" clearable />
            <img
              v-if="captchaUrl"
              :src="captchaUrl"
              alt="验证码"
              title="点击刷新"
              class="h-10 w-28 shrink-0 cursor-pointer rounded-lg border border-gray-200 object-cover"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="mt-1 w-full" :loading="loading" @click="onSubmit">
          登 录
        </el-button>
      </el-form>

      <p class="mt-6 text-center text-xs text-gray-300">密码连续错误将锁定账号，请联系管理员处理</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElForm, ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCaptcha } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaUrl = ref('')

const form = reactive({
  account: '',
  password: '',
  captchaId: '',
  captchaCode: ''
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入工号或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/** 拉取图形验证码 */
async function refreshCaptcha() {
  try {
    const captcha = await getCaptcha()
    form.captchaId = captcha.captchaId
    form.captchaCode = ''
    captchaUrl.value = captcha.imageBase64
  } catch (e) {
    ElMessage.error((e as Error).message || '验证码获取失败')
  }
}

/** 提交登录 */
async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login({ ...form })
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch (e) {
    ElMessage.error((e as Error).message || '登录失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>
