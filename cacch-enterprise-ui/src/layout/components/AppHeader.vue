<template>
  <header class="flex h-14 shrink-0 items-center gap-4 border-b border-gray-100 bg-white px-5">
    <!-- 左侧：折叠入口与当前页面 -->
    <button type="button" class="text-gray-500 hover:text-gray-700">
      <BaseIcon name="panel" :size="18" />
    </button>
    <span class="text-[15px] font-medium text-gray-900">工作台</span>

    <!-- 全局搜索 -->
    <div
      class="mx-auto flex w-full max-w-md cursor-text items-center gap-2 rounded-lg border border-gray-100 bg-gray-50 px-3 py-1.5 text-[13px] text-gray-400"
    >
      <BaseIcon name="search" :size="15" />
      <span class="flex-1 truncate">搜索应用、制度，或直接问 AI</span>
      <span class="rounded border border-gray-200 bg-white px-1 py-0.5 text-[10px] text-gray-400">⌘ K</span>
    </div>

    <!-- 右侧：通知与用户 -->
    <div class="flex items-center gap-4">
      <button type="button" class="relative text-gray-500 hover:text-gray-700">
        <BaseIcon name="bell" :size="18" />
        <span class="absolute -right-0.5 -top-0.5 h-2 w-2 rounded-full bg-rose-500"></span>
      </button>
      <div class="flex cursor-pointer items-center gap-2">
        <span
          class="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600"
        >
          悦
        </span>
        <span class="text-[13px] text-gray-700">林悦</span>
        <BaseIcon name="chevron-down" :size="14" class="text-gray-400" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import BaseIcon from '../../components/BaseIcon.vue'
</script>
