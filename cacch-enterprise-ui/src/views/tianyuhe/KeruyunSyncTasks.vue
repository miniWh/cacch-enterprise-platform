<template>
  <div class="p-6">
    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <!-- 操作栏：手动触发跨天同步 -->
      <div class="mb-4 flex flex-wrap items-center gap-3">
        <span class="text-xs text-gray-400">提示：单次同步时间跨度不得超过 31 天</span>
        <div class="ml-auto flex items-center gap-3">
          <el-date-picker
            v-model="syncRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="同步开始时间"
            end-placeholder="同步结束时间"
            class="sync-range-picker"
            @change="onSyncRangeChange"
          />
          <el-button type="primary" :icon="RefreshRight" :loading="triggering" @click="onTriggerSync">
            触发跨天同步
          </el-button>
        </div>
      </div>

      <!-- 同步记录表 -->
      <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
        <el-table-column prop="syncDate" label="同步日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ formatStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="查询窗口" min-width="320">
          <template #default="{ row }">
            <span class="text-xs text-gray-600">{{ formatDateTime(row.syncWindowStart) }}</span>
            <span class="mx-1 text-gray-400">~</span>
            <span class="text-xs text-gray-600">{{ formatDateTime(row.syncWindowEnd) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总订单数" width="100" align="right" />
        <el-table-column prop="successCount" label="成功数" width="90" align="right">
          <template #default="{ row }">
            <span class="text-green-600">{{ row.successCount ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="failureCount" label="失败数" width="90" align="right">
          <template #default="{ row }">
            <span class="text-red-600">{{ row.failureCount ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="90" align="right" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="whitespace-nowrap">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="170">
          <template #default="{ row }">
            <span class="whitespace-nowrap">{{ formatDateTime(row.syncedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="失败原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchTasks"
          @current-change="fetchTasks"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import {
  pageSyncTasks,
  triggerManualSync,
  type KeruyunSyncTaskVO,
  type KeruyunSyncTaskStatus
} from '../../api/keruyun'

// ---------- 状态枚举映射 ----------
const STATUS_MAP: Record<KeruyunSyncTaskStatus, string> = {
  PENDING: '待执行',
  IN_PROGRESS: '执行中',
  SUCCESS: '成功',
  FAILED: '失败',
  PARTIAL: '部分失败'
}

const STATUS_TAG_MAP: Record<KeruyunSyncTaskStatus, string> = {
  PENDING: 'info',
  IN_PROGRESS: 'primary',
  SUCCESS: 'success',
  FAILED: 'danger',
  PARTIAL: 'warning'
}

/** 状态转中文 */
function formatStatus(status: KeruyunSyncTaskStatus): string {
  return STATUS_MAP[status] ?? status
}

/** 状态对应的 el-tag 类型 */
function statusTagType(status: KeruyunSyncTaskStatus): string {
  return STATUS_TAG_MAP[status] ?? 'info'
}

/** 是否为终态（轮询结束条件） */
function isTerminalStatus(status: KeruyunSyncTaskStatus): boolean {
  return status === 'SUCCESS' || status === 'FAILED' || status === 'PARTIAL'
}

// ---------- 时间格式化 ----------
/** ISO 时间转 yyyy-MM-dd HH:mm:ss（去除 T 分隔与小数秒） */
function formatDateTime(value?: string): string {
  if (!value) return '-'
  return value.replace('T', ' ').replace(/\.\d+$/, '')
}

/** 将 Date 格式化为 yyyy-MM-ddTHH:mm:ss */
function formatToLocalDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ---------- 同步记录列表 ----------
const rows = ref<KeruyunSyncTaskVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

async function fetchTasks() {
  loading.value = true
  try {
    const res = await pageSyncTasks(pageNum.value, pageSize.value)
    rows.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

// ---------- 手动触发跨天同步 ----------
const syncRange = ref<[Date, Date] | null>(null)
const triggering = ref(false)
const MAX_DAYS = 31
const ONE_DAY_MS = 24 * 60 * 60 * 1000

/** 时间区间变化时校验跨度不超过 31 天 */
function onSyncRangeChange(val: [Date, Date] | null) {
  if (!val || val.length !== 2) return
  const [start, end] = val
  const diffDays = (end.getTime() - start.getTime()) / ONE_DAY_MS
  if (diffDays > MAX_DAYS) {
    ElMessage.error(`同步时间跨度不得超过 ${MAX_DAYS} 天，请缩小范围`)
    syncRange.value = null
  }
}

async function onTriggerSync() {
  if (!syncRange.value || syncRange.value.length !== 2) {
    ElMessage.warning('请先选择同步时间区间')
    return
  }
  const [start, end] = syncRange.value
  const diffDays = (end.getTime() - start.getTime()) / ONE_DAY_MS
  if (diffDays > MAX_DAYS) {
    ElMessage.error(`同步时间跨度不得超过 ${MAX_DAYS} 天，请缩小范围`)
    return
  }

  triggering.value = true
  try {
    const result = await triggerManualSync(
      formatToLocalDateTime(start),
      formatToLocalDateTime(end)
    )
    ElMessage.success(result.message || '跨天同步已触发')
    // 触发后刷新列表并启动轮询
    pageNum.value = 1
    fetchTasks()
    startPolling()
  } catch (e) {
    // 错误已由全局拦截器提示
  } finally {
    triggering.value = false
  }
}

// ---------- 轮询（触发同步后自动刷新状态） ----------
let pollTimer: ReturnType<typeof setInterval> | null = null
let pollCount = 0
const MAX_POLL_COUNT = 60 // 最长轮询约 3 分钟（每 3 秒一次）

function startPolling() {
  stopPolling()
  pollCount = 0
  pollTimer = setInterval(async () => {
    pollCount++
    await fetchTasks()
    // 检查最新一条任务是否已到终态
    const latest = rows.value[0]
    if (latest && isTerminalStatus(latest.status)) {
      stopPolling()
      ElMessage.info(`同步任务已完成：${formatStatus(latest.status)}`)
    } else if (pollCount >= MAX_POLL_COUNT) {
      stopPolling()
      ElMessage.warning('同步任务仍在执行中，请稍后刷新查看')
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchTasks()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
/* 时间区间选择器宽度收缩：根节点自动收缩，输入框按 19 位时间字符宽度自适应 */
.sync-range-picker {
  width: auto;
}
.sync-range-picker :deep(.el-range-input) {
  width: 19ch;
}
</style>
