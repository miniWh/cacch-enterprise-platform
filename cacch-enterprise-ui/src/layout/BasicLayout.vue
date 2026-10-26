<template>
  <div class="flex h-screen overflow-hidden bg-[#f6f7f9]">
    <AppSidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <AppHeader />
      <main class="min-h-0 flex-1 overflow-y-auto">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'

// 后台基础布局：左侧导航 + 顶栏 + 内容区（路由接入后可替换 slot 为 router-view）
</script>
