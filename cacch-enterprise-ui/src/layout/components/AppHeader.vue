<template>
  <header class="flex h-14 shrink-0 items-center gap-4 border-b border-gray-100 bg-white px-5">
    <!-- 当前页面标题 -->
    <span class="text-[15px] font-medium text-gray-900">{{ pageTitle }}</span>

    <div class="ml-auto flex items-center gap-4">
      <div class="flex cursor-pointer items-center gap-2">
        <span
          class="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600"
        >
          {{ avatarChar }}
        </span>
        <span class="text-[13px] text-gray-700">{{ auth.profile?.realName ?? '-' }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <BaseIcon name="chevron-down" :size="14" class="text-gray-400" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseIcon from '../../components/BaseIcon.vue'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 页面标题（路由 meta） */
const pageTitle = computed(() => (route.meta?.title as string) ?? '工作台')

/** 头像字符（姓名末字） */
const avatarChar = computed(() => {
  const name = auth.profile?.realName
  return name ? name.slice(-1) : '-'
})

/** 下拉命令分发 */
async function onCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await auth.logout()
    router.replace('/login')
  }
}
</script>
