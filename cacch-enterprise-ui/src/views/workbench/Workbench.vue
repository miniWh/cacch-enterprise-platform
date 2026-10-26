<template>
  <div class="mx-auto max-w-[1200px] px-6 py-6">
    <!-- 问候与日期 -->
    <div class="flex items-end justify-between">
      <div>
        <h1 class="text-xl font-semibold text-gray-900">早上好，林悦</h1>
        <p class="mt-1 text-[13px] text-gray-400">让琐碎事务更简单，把时间留给重要的事</p>
      </div>
      <div class="text-right">
        <p class="text-[13px] text-gray-500">2026年10月21日</p>
        <p class="mt-0.5 text-xs text-gray-400">星期三</p>
      </div>
    </div>

    <!-- AI 今日摘要 -->
    <section
      class="mt-5 rounded-xl border border-indigo-100 bg-gradient-to-r from-indigo-50 via-indigo-50/70 to-violet-50 p-4"
    >
      <div class="flex items-center gap-2">
        <span
          class="flex h-8 w-8 items-center justify-center rounded-lg border border-indigo-100/80 bg-white text-indigo-500 shadow-sm"
        >
          <BaseIcon name="sparkles" :size="17" />
        </span>
        <span class="text-sm font-semibold text-gray-900">AI 今日摘要</span>
        <span class="rounded bg-indigo-100/80 px-1.5 py-0.5 text-[10px] text-indigo-500">AI 生成</span>
        <span class="ml-auto text-xs text-gray-400">根据今日工作动态整理</span>
      </div>
      <p class="mt-3 text-sm leading-6 text-gray-700">
        你有 <span class="font-semibold text-gray-900">8</span> 项待办，其中
        <span class="font-semibold text-gray-900">3</span> 项需要优先处理；今天安排了
        <span class="font-semibold text-gray-900">2</span> 场会议，最近一场将在
        <span class="font-semibold text-gray-900">10:30</span> 开始
      </p>
      <div class="mt-2 flex items-center gap-5 text-[13px] font-medium text-indigo-600">
        <a href="javascript:;" class="flex items-center gap-1">查看待办 <BaseIcon name="arrow-right" :size="13" /></a>
        <a href="javascript:;" class="flex items-center gap-1">查看日程 <BaseIcon name="arrow-right" :size="13" /></a>
      </div>
    </section>

    <!-- 统计卡片 -->
    <section class="mt-4 grid grid-cols-4 gap-4">
      <div v-for="card in statCards" :key="card.label" class="rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div class="flex items-start justify-between">
          <span class="text-[13px] text-gray-500">{{ card.label }}</span>
          <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-50 text-indigo-500">
            <BaseIcon :name="card.icon" :size="16" />
          </span>
        </div>
        <p class="mt-2 text-[26px] font-semibold leading-8 text-gray-900">
          {{ card.value }}
          <span v-if="card.unit" class="ml-0.5 text-sm font-normal text-gray-500">{{ card.unit }}</span>
        </p>
      </div>
    </section>

    <!-- 主体两栏 -->
    <section class="mt-4 grid grid-cols-[minmax(0,1fr)_320px] items-start gap-4">
      <!-- 左栏 -->
      <div class="flex min-w-0 flex-col gap-4">
        <!-- 待办审批 -->
        <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <h2 class="text-[15px] font-semibold text-gray-900">待办审批</h2>
            <a
              href="javascript:;"
              class="flex items-center gap-1 text-[13px] text-gray-500 hover:text-indigo-600"
            >
              查看全部 <BaseIcon name="arrow-right" :size="13" />
            </a>
          </div>
          <div class="mt-3 flex items-center gap-2 text-[13px]">
            <span class="rounded-md bg-indigo-50 px-3 py-1 font-medium text-indigo-600">全部</span>
            <span class="cursor-pointer px-3 py-1 text-gray-500 hover:text-gray-700">待审批</span>
            <span class="cursor-pointer px-3 py-1 text-gray-500 hover:text-gray-700">待处理</span>
          </div>
          <ul class="mt-1">
            <li
              v-for="todo in todoItems"
              :key="todo.title"
              class="flex cursor-pointer items-center gap-3 border-b border-gray-50 py-3 last:border-0"
            >
              <span
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-medium"
                :class="todo.avatarClass"
              >
                {{ todo.avatarChar }}
              </span>
              <div class="min-w-0 flex-1">
                <p class="truncate text-[13px] font-medium text-gray-800">{{ todo.title }}</p>
                <p class="mt-0.5 truncate text-xs text-gray-400">{{ todo.meta }}</p>
              </div>
              <span class="shrink-0 rounded-md px-2 py-0.5 text-xs" :class="todo.tagClass">{{ todo.tag }}</span>
              <span class="w-20 shrink-0 text-right text-xs text-gray-400">{{ todo.time }}</span>
              <BaseIcon name="chevron-right" :size="15" class="shrink-0 text-gray-300" />
            </li>
          </ul>
          <p class="mt-2 text-center text-xs text-gray-300">展示 4 项待审批中的 3 项，还有 1 项待处理</p>
        </div>

        <!-- 常用应用 -->
        <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <h2 class="text-[15px] font-semibold text-gray-900">常用应用</h2>
            <a
              href="javascript:;"
              class="flex items-center gap-1 text-[13px] text-gray-500 hover:text-indigo-600"
            >
              全部应用 <BaseIcon name="arrow-right" :size="13" />
            </a>
          </div>
          <div class="mt-4 grid grid-cols-6 gap-2">
            <a
              v-for="app in apps"
              :key="app.label"
              href="javascript:;"
              class="flex flex-col items-center gap-2 rounded-lg py-2 hover:bg-gray-50"
            >
              <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-50 text-indigo-500">
                <BaseIcon :name="app.icon" :size="20" />
              </span>
              <span class="text-xs text-gray-600">{{ app.label }}</span>
            </a>
          </div>
        </div>

        <!-- 企业公告 -->
        <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <h2 class="text-[15px] font-semibold text-gray-900">企业公告</h2>
            <a
              href="javascript:;"
              class="flex items-center gap-1 text-[13px] text-gray-500 hover:text-indigo-600"
            >
              查看全部 <BaseIcon name="arrow-right" :size="13" />
            </a>
          </div>
          <ul class="mt-2">
            <li
              v-for="notice in notices"
              :key="notice.title"
              class="flex cursor-pointer items-center gap-3 border-b border-gray-50 py-3 last:border-0"
            >
              <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gray-100 text-gray-500">
                <BaseIcon name="megaphone" :size="16" />
              </span>
              <p class="min-w-0 flex-1 truncate text-[13px] text-gray-700">{{ notice.title }}</p>
              <span class="shrink-0 text-xs text-gray-400">{{ notice.date }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- 右栏 -->
      <div class="flex min-w-0 flex-col gap-4">
        <!-- 今日日程 -->
        <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <h2 class="text-[15px] font-semibold text-gray-900">今日日程</h2>
            <a
              href="javascript:;"
              class="flex items-center gap-1 text-[13px] text-gray-500 hover:text-indigo-600"
            >
              查看日程 <BaseIcon name="arrow-right" :size="13" />
            </a>
          </div>
          <div class="mt-4 flex flex-col gap-3">
            <div
              v-for="meeting in meetings"
              :key="meeting.title"
              class="rounded-lg border-l-2 border-blue-500 bg-gray-50 px-3 py-2.5"
            >
              <p class="text-xs font-medium text-gray-500">{{ meeting.time }}</p>
              <p class="mt-1 text-[13px] font-medium text-gray-800">{{ meeting.title }}</p>
              <p class="mt-1 flex items-center gap-1 text-xs text-gray-400">
                <BaseIcon :name="meeting.icon" :size="12" />
                {{ meeting.place }}
              </p>
            </div>
          </div>
        </div>

        <!-- AI 助手 -->
        <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
          <div class="flex items-center gap-2">
            <span class="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 text-indigo-500">
              <BaseIcon name="sparkles" :size="15" />
            </span>
            <h2 class="text-[15px] font-semibold text-gray-900">AI 助手</h2>
            <span class="ml-auto flex items-center gap-1 text-xs text-gray-400">
              <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
              在线
            </span>
          </div>

          <div class="mt-4 grid grid-cols-3 gap-2">
            <button
              v-for="action in quickActions"
              :key="action.label"
              type="button"
              class="flex flex-col items-center gap-1.5 rounded-lg bg-gray-50 py-3 text-xs text-gray-600 hover:bg-indigo-50 hover:text-indigo-600"
            >
              <BaseIcon :name="action.icon" :size="18" />
              {{ action.label }}
            </button>
          </div>

          <p class="mt-4 text-xs text-gray-400">你可以这样问</p>
          <ul class="mt-2 divide-y divide-gray-50 rounded-lg border border-gray-100">
            <li
              v-for="suggestion in suggestions"
              :key="suggestion"
              class="flex cursor-pointer items-center justify-between px-3 py-2 text-[13px] text-gray-600 hover:bg-gray-50"
            >
              {{ suggestion }}
              <BaseIcon name="arrow-up-right" :size="13" class="text-gray-300" />
            </li>
          </ul>

          <div class="mt-3 flex items-center gap-2">
            <div
              class="flex h-9 flex-1 cursor-text items-center rounded-lg border border-gray-200 bg-gray-50 px-3 text-[13px] text-gray-400"
            >
              提问咨询，交给 AI 处理
            </div>
            <button
              type="button"
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-indigo-600 text-white hover:bg-indigo-500"
            >
              <BaseIcon name="arrow-up" :size="15" />
            </button>
          </div>
          <p class="mt-2 text-[11px] text-gray-300">涉及正式业务操作，需以人工确认为准</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import BaseIcon from '../../components/BaseIcon.vue'

// 统计卡片（静态演示数据）
const statCards = [
  { label: '待我审批', value: '8', unit: '', icon: 'clipboard' },
  { label: '我的申请', value: '3', unit: '', icon: 'file' },
  { label: '今日会议', value: '2', unit: '', icon: 'calendar' },
  { label: '剩余年假', value: '5', unit: '天', icon: 'coffee' }
]

// 待办审批列表（静态演示数据）
const todoItems = [
  {
    avatarChar: '陈',
    avatarClass: 'bg-indigo-50 text-indigo-600',
    title: '陈周的差旅报销 ¥2,680',
    meta: '财务报销',
    tag: '待审批',
    tagClass: 'bg-indigo-50 text-indigo-600',
    time: '今天 09:12'
  },
  {
    avatarChar: '王',
    avatarClass: 'bg-violet-50 text-violet-600',
    title: '王敏的年假申请',
    meta: '10月23日–10月24日 · 请假申请',
    tag: '待审批',
    tagClass: 'bg-indigo-50 text-indigo-600',
    time: '今天 08:45'
  },
  {
    avatarChar: '李',
    avatarClass: 'bg-emerald-50 text-emerald-600',
    title: '李剑的笔记本电脑领用',
    meta: '领用申请',
    tag: '待审批',
    tagClass: 'bg-indigo-50 text-indigo-600',
    time: '昨天 17:30'
  },
  {
    avatarChar: '林',
    avatarClass: 'bg-sky-50 text-sky-600',
    title: '林本伟补充的9月出差发票',
    meta: '报销补充',
    tag: '待处理',
    tagClass: 'bg-amber-50 text-amber-600',
    time: '昨天 16:20'
  }
]

// 常用应用（静态演示数据）
const apps = [
  { icon: 'clock', label: '考勤打卡' },
  { icon: 'pen', label: '发起审批' },
  { icon: 'send', label: '请假申请' },
  { icon: 'receipt', label: '费用报销' },
  { icon: 'calendar', label: '会议室预订' },
  { icon: 'headset', label: 'IT 服务' }
]

// 企业公告（静态演示数据）
const notices = [
  { title: '2026年度健康体检安排', date: '10月26日' },
  { title: '办公区门禁升级通知', date: '10月19日' }
]

// 今日日程（静态演示数据）
const meetings = [
  { time: '10:30–11:00', title: '产品晨会', icon: 'pin', place: '会议室 A301' },
  { time: '14:00–15:00', title: '新员工入职培训', icon: 'message', place: '线上会议' }
]

// AI 助手快捷操作与推荐提问（静态演示数据）
const quickActions = [
  { icon: 'message', label: '制度问答' },
  { icon: 'file', label: '审批摘要' },
  { icon: 'pen', label: '会议纪要' }
]
const suggestions = ['我还有多少年假？', '总结待审批事项', '查找差旅报销标准']
</script>
