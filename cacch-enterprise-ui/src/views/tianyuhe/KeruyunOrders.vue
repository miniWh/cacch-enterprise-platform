<template>
  <div class="p-6">
    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <!-- 查询条件 -->
      <div class="flex flex-wrap items-center gap-3">
        <el-input v-model="query.orderId" placeholder="订单ID" clearable class="!w-44" @keyup.enter="fetchPage" />
        <el-input v-model="query.shopId" placeholder="门店ID" clearable class="!w-36" @keyup.enter="fetchPage" />
        <el-select v-model="query.orderStatus" placeholder="订单状态" clearable class="!w-36" @change="onSearch">
          <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.orderType" placeholder="订单类型" clearable class="!w-36" @change="onSearch">
          <el-option v-for="item in ORDER_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="openTimeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="下单开始"
          end-placeholder="下单结束"
          class="!w-[380px]"
        />
        <el-button type="primary" :icon="Search" :loading="loading" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        <div class="ml-auto">
          <el-button type="success" :icon="Download" :loading="exporting" @click="onExport">导出 Excel</el-button>
        </div>
      </div>

      <!-- 列表 -->
      <el-table :data="rows" v-loading="loading" class="mt-4" stripe border>
        <el-table-column prop="orderId" label="订单ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="shopId" label="门店ID" width="110" />
        <el-table-column prop="busiOrderNo" label="业务订单号" min-width="150" show-overflow-tooltip />
        <el-table-column label="订单状态" width="110">
          <template #default="{ row }">{{ formatOrderStatus(row.orderStatus) }}</template>
        </el-table-column>
        <el-table-column label="订单类型" width="110">
          <template #default="{ row }">{{ formatOrderType(row.orderType) }}</template>
        </el-table-column>
        <el-table-column label="订单金额(元)" width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.orderAmtYuan) }}</template>
        </el-table-column>
        <el-table-column label="优惠金额(元)" width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.promoAmtYuan) }}</template>
        </el-table-column>
        <el-table-column label="实收金额(元)" width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.orderReceivedAmtYuan) }}</template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.openTime) }}</template>
        </el-table-column>
        <el-table-column label="完结时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.finishTime) }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="mt-4 justify-end"
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        @change="fetchPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { Search, RefreshLeft, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  pageOrders,
  exportOrders,
  type KeruyunOrderQuery,
  type KeruyunOrderVO
} from '../../api/keruyun'

// ---------- 枚举映射（英文枚举 → 中文，不含数字序号） ----------
const ORDER_STATUS_MAP: Record<string, string> = {
  WAIT_PROCESSED: '待处理',
  WAIT_SETTLED: '待结账',
  SETTLED: '已结账',
  REFUND: '已退单',
  INVALID: '已作废',
  CANCELLED: '已取消',
  REJECTED: '已拒绝',
  SUCCESS: '已完成'
}

const ORDER_TYPE_MAP: Record<string, string> = {
  FOR_HERE: '堂食',
  PLATFORM_TAKE_OUT: '平台外卖',
  SELF_TAKE_OUT: '自管外卖',
  MEMBER_STORE: '会员充值',
  NO_ORDER_CASHIER: '无单收银',
  REPAYMENT_ORDER: '销账订单'
}

const ORDER_STATUS_OPTIONS = Object.entries(ORDER_STATUS_MAP).map(([value, label]) => ({ value, label }))
const ORDER_TYPE_OPTIONS = Object.entries(ORDER_TYPE_MAP).map(([value, label]) => ({ value, label }))

/** 订单状态转中文，未知枚举原样返回 */
function formatOrderStatus(value?: string): string {
  if (!value) return '-'
  return ORDER_STATUS_MAP[value] ?? value
}

/** 订单类型转中文，未知枚举原样返回 */
function formatOrderType(value?: string): string {
  if (!value) return '-'
  return ORDER_TYPE_MAP[value] ?? value
}

// ---------- 查询条件 ----------
const query = reactive<KeruyunOrderQuery>({
  orderId: '',
  shopId: '',
  orderStatus: '',
  orderType: '',
  openTimeFrom: undefined,
  openTimeTo: undefined
})

/** 下单时间区间选择器绑定值（Date 对象数组） */
const openTimeRange = ref<[Date, Date] | null>(null)

/** 将 Date 格式化为 yyyy-MM-ddTHH:mm:ss */
function formatToLocalDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 将区间选择器的 Date 值同步到 query，格式化为接口要求的 yyyy-MM-ddTHH:mm:ss */
function syncRanges() {
  query.openTimeFrom = openTimeRange.value?.[0] ? formatToLocalDateTime(openTimeRange.value[0]) : undefined
  query.openTimeTo = openTimeRange.value?.[1] ? formatToLocalDateTime(openTimeRange.value[1]) : undefined
}

/** 构造实际请求参数（剔除空值） */
function buildQueryParams(): KeruyunOrderQuery {
  syncRanges()
  const params: KeruyunOrderQuery = {}
  ;(Object.keys(query) as (keyof KeruyunOrderQuery)[]).forEach((k) => {
    const v = query[k]
    if (v !== '' && v !== undefined && v !== null) {
      ;(params as Record<string, unknown>)[k] = v
    }
  })
  return params
}

// ---------- 列表与分页 ----------
const rows = ref<KeruyunOrderVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

async function fetchPage() {
  loading.value = true
  try {
    const res = await pageOrders(buildQueryParams(), pageNum.value, pageSize.value)
    rows.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  fetchPage()
}

function onReset() {
  query.orderId = ''
  query.shopId = ''
  query.orderStatus = ''
  query.orderType = ''
  openTimeRange.value = null
  pageNum.value = 1
  fetchPage()
}

// ---------- 导出 ----------
const exporting = ref(false)

/** Excel 列定义（与表格展示列一致） */
const EXPORT_COLUMNS: { key: keyof KeruyunOrderVO; label: string }[] = [
  { key: 'orderId', label: '订单ID' },
  { key: 'shopId', label: '门店ID' },
  { key: 'busiOrderNo', label: '业务订单号' },
  { key: 'orderStatus', label: '订单状态' },
  { key: 'orderType', label: '订单类型' },
  { key: 'orderAmtYuan', label: '订单金额(元)' },
  { key: 'promoAmtYuan', label: '优惠金额(元)' },
  { key: 'orderReceivedAmtYuan', label: '实收金额(元)' },
  { key: 'openTime', label: '下单时间' },
  { key: 'finishTime', label: '完结时间' }
]

async function onExport() {
  exporting.value = true
  try {
    const list = await exportOrders(buildQueryParams())
    if (!list || list.length === 0) {
      ElMessage.warning('当前条件下无数据可导出')
      return
    }
    const aoa: unknown[][] = [
      EXPORT_COLUMNS.map((c) => c.label),
      ...list.map((row) =>
        EXPORT_COLUMNS.map((c) => {
          const v = row[c.key]
          if (c.key === 'openTime' || c.key === 'finishTime') {
            return formatDateTime(v as string)
          }
          if (c.key === 'orderStatus') {
            return formatOrderStatus(v as string)
          }
          if (c.key === 'orderType') {
            return formatOrderType(v as string)
          }
          return v ?? ''
        })
      )
    ]
    const ws = XLSX.utils.aoa_to_sheet(aoa)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '客如云订单')
    const fileName = `客如云订单_${formatFileTimestamp()}.xlsx`
    XLSX.writeFile(wb, fileName)
    ElMessage.success(`已导出 ${list.length} 条数据`)
  } catch (e) {
    // 超限（code=40001）等业务错误由 http 拦截器 reject，消息取自 response.message
    const msg = e instanceof Error ? e.message : '导出失败'
    ElMessage.error(msg)
  } finally {
    exporting.value = false
  }
}

// ---------- 工具方法 ----------
/** 格式化 LocalDateTime 字符串为 yyyy-MM-dd HH:mm:ss */
function formatDateTime(value?: string): string {
  if (!value) return '-'
  // 接口返回 ISO-8601，如 2026-10-21T12:30:00
  return value.replace('T', ' ').substring(0, 19)
}

/** 金额格式化：保留 2 位小数，空值显示 - */
function formatAmount(value?: number): string {
  if (value === null || value === undefined) return '-'
  return value.toFixed(2)
}

/** 导出文件名时间戳：yyyyMMddHHmmss */
function formatFileTimestamp(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

// 初始加载
fetchPage()
</script>
