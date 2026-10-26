<template>
  <aside class="flex w-60 shrink-0 flex-col border-r border-gray-100 bg-white px-3 py-4">
    <!-- 品牌区 -->
    <div class="flex items-center gap-2 px-2">
      <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-white">
        <BaseIcon name="logo" :size="18" />
      </span>
      <span class="text-[15px] font-semibold text-gray-900">同势 · 智能办公</span>
    </div>

    <!-- 工作区切换 -->
    <button
      type="button"
      class="mt-4 flex items-center gap-2 rounded-lg border border-gray-100 bg-gray-50 px-3 py-2 text-[13px] text-gray-700 hover:border-gray-200"
    >
      <BaseIcon name="briefcase" :size="15" class="text-gray-400" />
      <span>同势科技</span>
      <BaseIcon name="chevron-down" :size="14" class="ml-auto text-gray-400" />
    </button>

    <!-- 主导航 -->
    <nav class="mt-4 flex flex-col gap-1">
      <a
        v-for="item in navItems"
        :key="item.label"
        href="javascript:;"
        class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-gray-600"
        :class="item.active ? 'bg-indigo-50 font-medium text-indigo-600' : 'hover:bg-gray-50'"
      >
        <BaseIcon :name="item.icon" :size="17" />
        <span>{{ item.label }}</span>
        <span v-if="item.badge" class="ml-auto text-xs text-gray-400">{{ item.badge }}</span>
      </a>
    </nav>

    <!-- 底部辅助入口 -->
    <div class="mt-auto flex flex-col gap-1">
      <a
        v-for="item in footItems"
        :key="item.label"
        href="javascript:;"
        class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-[13px] text-gray-500 hover:bg-gray-50"
      >
        <BaseIcon :name="item.icon" :size="16" :class="item.iconClass" />
        <span>{{ item.label }}</span>
      </a>

      <!-- 当前用户 -->
      <div class="mt-3 flex items-center gap-2.5 border-t border-gray-100 px-2 pt-3">
        <span
          class="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600"
        >
          悦
        </span>
        <div class="min-w-0">
          <p class="text-[13px] font-medium text-gray-800">林悦</p>
          <p class="text-[11px] text-gray-400">同势科技</p>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import BaseIcon from '../../components/BaseIcon.vue'

// 主导航菜单（静态演示数据）
const navItems = [
  { icon: 'grid', label: '工作台', active: true },
  { icon: 'approval', label: '审批中心', badge: '8' },
  { icon: 'users', label: '人事管理' },
  { icon: 'briefcase', label: '行政服务' },
  { icon: 'book', label: '知识库' }
]

// 底部辅助入口
const footItems = [
  { icon: 'shield', label: '企业数据安全守护', iconClass: 'text-emerald-500' },
  { icon: 'sliders', label: '设置', iconClass: '' },
  { icon: 'help', label: '帮助', iconClass: '' }
]
</script>
