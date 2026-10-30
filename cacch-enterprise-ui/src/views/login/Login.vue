<template>
  <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-violet-50">
    <div v-loading="wecomLoading" element-loading-text="企微免登中..." class="w-[400px] rounded-2xl border border-gray-100 bg-white p-8 shadow-lg">
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
import { registerDynamicRoutes } from '../../router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const wecomLoading = ref(false)
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
    // 跳转前先加载用户信息并注册动态路由：目标路由未注册时会命中兜底路由
    // 被 matcher 层重定向回登录页，依赖二次导航救回的链路在部分状态下会中断
    await auth.loadProfile()
    registerDynamicRoutes()
    const redirect = (route.query.redirect as string) || '/dashboard'
    await router.replace(redirect)
  } catch (e) {
    ElMessage.error((e as Error).message || '登录失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

/** 企业微信自建应用免登：检测 URL 中的 code 参数，自动换取令牌并进入系统 */
async function tryWecomInAppLogin() {
  const code = route.query.code as string
  if (!code) return
  wecomLoading.value = true
  try {
    await auth.wecomLogin(code)
    await auth.loadProfile()
    registerDynamicRoutes()
    const redirect = (route.query.redirect as string) || '/dashboard'
    // 清除 URL 中的 code 与 redirect，避免刷新重复触发免登
    await router.replace({ path: '/login', query: {} })
    await router.replace(redirect)
  } catch (e) {
    // 免登失败（未绑定/授权码失效等）：保留登录页供手动登录，提示用户
    ElMessage.error((e as Error).message || '企微免登失败，请使用账号密码登录')
    // 清除 URL 中的 code，避免刷新重复触发
    router.replace({ path: '/login', query: {} })
  } finally {
    wecomLoading.value = false
  }
}

onMounted(async () => {
  await tryWecomInAppLogin()
  // 非企微免登场景或免登失败时加载验证码，供手动登录使用
  if (!wecomLoading.value) {
    refreshCaptcha()
  }
})
</script>
