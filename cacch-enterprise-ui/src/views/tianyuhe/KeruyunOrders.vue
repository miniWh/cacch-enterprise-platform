<template>
  <div class="mx-auto max-w-[1200px] px-6 py-6">
    <!-- 页面标题 -->
    <div class="flex items-end justify-between">
      <div>
        <h1 class="text-xl font-semibold text-gray-900">客如云订单</h1>
        <p class="mt-1 text-[13px] text-gray-400">田裕禾 · 客如云订单查询与管理</p>
      </div>
    </div>

    <!-- 开发中占位提示 -->
    <section class="mt-5 rounded-xl border border-gray-100 bg-white p-10 text-center shadow-sm">
      <span
        class="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50 text-indigo-500"
        style="margin: 0 auto"
      >
        <BaseIcon name="clipboard" :size="22" />
      </span>
      <h2 class="mt-4 text-[15px] font-semibold text-gray-900">页面开发中</h2>
      <p class="mt-2 text-[13px] text-gray-400">客如云订单对接与展示功能即将上线，敬请期待</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import BaseIcon from '../../components/BaseIcon.vue'
</script>
