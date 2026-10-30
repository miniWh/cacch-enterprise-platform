<template>
  <aside class="flex w-60 shrink-0 flex-col border-r border-gray-100 bg-white px-3 py-4">
    <!-- 品牌区 -->
    <div class="flex flex-col items-center gap-2 px-2">
      <img src="../../assets/logo-cac-group.png" alt="泰禾集团" class="h-9 w-auto" />
      <span class="text-[13px] font-semibold text-gray-900">Cacch · 企业平台</span>
    </div>

    <!-- 动态菜单（后端菜单树驱动） -->
    <el-menu
      class="mt-4 flex-1 border-0"
      :default-active="route.path"
      router
      unique-opened
    >
      <template v-for="menu in visibleMenus" :key="menu.id">
        <!-- 目录 -->
        <el-sub-menu v-if="menu.menuType === 'D'" :index="menu.path || String(menu.id)">
          <template #title>
            <span>{{ menu.menuName }}</span>
          </template>
          <el-menu-item
            v-for="child in menu.children ?? []"
            :key="child.id"
            :index="child.path || String(child.id)"
          >
            {{ child.menuName }}
          </el-menu-item>
        </el-sub-menu>
        <!-- 菜单 -->
        <el-menu-item v-else :index="menu.path || String(menu.id)">
          {{ menu.menuName }}
        </el-menu-item>
      </template>
    </el-menu>

    <!-- 当前用户（点击进入个人中心） -->
    <div
      class="mt-3 flex cursor-pointer items-center gap-2.5 rounded-lg border-t border-gray-100 px-2 pt-3 transition-colors hover:bg-gray-50"
      title="进入个人中心"
      @click="router.push('/profile')"
    >
      <span
        class="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600"
      >
        {{ avatarChar }}
      </span>
      <div class="min-w-0">
        <p class="truncate text-[13px] font-medium text-gray-800">{{ auth.profile?.realName ?? '-' }}</p>
        <p class="text-[11px] text-gray-400">{{ auth.profile?.username ?? '' }}</p>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 可见菜单（visible=0） */
const visibleMenus = computed(() => auth.menus.filter((m) => m.visible === 0))

/** 头像字符（姓名末字） */
const avatarChar = computed(() => {
  const name = auth.profile?.realName
  return name ? name.slice(-1) : '-'
})
</script>
